package com.booksharing.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.entity.User;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.repository.UserRepository;
import com.booksharing.support.TestData;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("JWT-автентифікація")
class JwtSecurityTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-algorithm";

    private final JwtService jwtService = new JwtService(SECRET, 60);

    @Nested
    @DisplayName("видача й перевірка токена")
    class Tokens {

        @Test
        @DisplayName("токен містить id, email і ролі")
        void roundTrip() {
            UUID userId = UUID.randomUUID();
            String token = jwtService.generateToken(userId, "a@b.ua", List.of("USER"));

            Claims claims = jwtService.parseClaims(token);

            assertThat(claims.getSubject()).isEqualTo(userId.toString());
            assertThat(claims.get("email", String.class)).isEqualTo("a@b.ua");
            assertThat(claims.get("roles", List.class)).containsExactly("USER");
            assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
            assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        }

        @Test
        @DisplayName("токен з іншим ключем або зіпсований не приймається")
        void invalidToken() {
            JwtService other = new JwtService("another-secret-key-that-is-long-enough-for-hs256-alg", 60);
            String foreign = other.generateToken(UUID.randomUUID(), "a@b.ua", List.of());
            assertThatThrownBy(() -> jwtService.parseClaims(foreign)).isInstanceOf(JwtException.class);
            assertThatThrownBy(() -> jwtService.parseClaims("not.a.token")).isInstanceOf(JwtException.class);
        }

        @Test
        @DisplayName("прострочений токен не приймається")
        void expired() {
            JwtService shortLived = new JwtService(SECRET, -1);
            String token = shortLived.generateToken(UUID.randomUUID(), "a@b.ua", List.of());
            assertThatThrownBy(() -> jwtService.parseClaims(token)).isInstanceOf(ExpiredJwtException.class);
        }
    }

    @Nested
    @DisplayName("фільтр HTTP-запитів")
    class Filter {

        private final UserRepository userRepository = mock(UserRepository.class);
        private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService, userRepository);

        @AfterEach
        void clear() {
            SecurityContextHolder.clearContext();
        }

        private Authentication run(String header) throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/users/me");
            if (header != null) {
                request.addHeader(HttpHeaders.AUTHORIZATION, header);
            }
            FilterChain chain = mock(FilterChain.class);
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(request, response, chain);
            verify(chain).doFilter(request, response);
            return SecurityContextHolder.getContext().getAuthentication();
        }

        @Test
        @DisplayName("дійсний токен автентифікує користувача з його роллю")
        void validToken() throws Exception {
            User user = TestData.user("Модератор", UserRole.MODERATOR);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

            Authentication auth = run("Bearer " + jwtService.generateToken(user.getId(), user.getEmail(), List.of()));

            assertThat(auth.getPrincipal()).isEqualTo(user);
            assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_MODERATOR");
        }

        @Test
        @DisplayName("заблокований користувач не автентифікується")
        void blockedUser() throws Exception {
            User user = TestData.user("Заблокований");
            user.setStatus(UserStatus.BLOCKED);
            when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            assertThat(run("Bearer " + jwtService.generateToken(user.getId(), user.getEmail(), List.of()))).isNull();
        }

        @Test
        @DisplayName("без заголовка, з іншою схемою чи зіпсованим токеном запит іде далі анонімно")
        void anonymous() throws Exception {
            assertThat(run(null)).isNull();
            assertThat(run("Basic abc")).isNull();
            assertThat(run("Bearer broken")).isNull();
        }
    }

    @Nested
    @DisplayName("STOMP-з'єднання")
    class Stomp {

        private final StompAuthChannelInterceptor interceptor = new StompAuthChannelInterceptor(jwtService);
        private final MessageChannel channel = mock(MessageChannel.class);

        private Message<byte[]> frame(StompCommand command, String authorization, String destination, boolean withUser) {
            StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
            if (authorization != null) {
                accessor.addNativeHeader(HttpHeaders.AUTHORIZATION, authorization);
            }
            if (destination != null) {
                accessor.setDestination(destination);
            }
            if (withUser) {
                accessor.setUser(new UsernamePasswordAuthenticationToken("user", null, List.of()));
            }
            accessor.setLeaveMutable(true);
            return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        }

        @Test
        @DisplayName("підключення з дійсним токеном прив'язує користувача")
        void connect() {
            UUID userId = UUID.randomUUID();
            Message<byte[]> message = frame(StompCommand.CONNECT,
                    "Bearer " + jwtService.generateToken(userId, "a@b.ua", List.of()), null, false);

            interceptor.preSend(message, channel);

            StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            assertThat(accessor.getUser().getName()).isEqualTo(userId.toString());
        }

        @Test
        @DisplayName("підключення без токена або з недійсним токеном відхиляється")
        void connectRejected() {
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, null, null, false), channel))
                    .isInstanceOf(MessagingException.class);
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "Token x", null, false), channel))
                    .isInstanceOf(MessagingException.class);
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.CONNECT, "Bearer broken", null, false), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("недійсний");
        }

        @Test
        @DisplayName("підписка дозволена лише на власні черги")
        void subscribe() {
            Message<byte[]> allowed = frame(StompCommand.SUBSCRIBE, null, "/user/queue/chat", true);
            assertThat(interceptor.preSend(allowed, channel)).isSameAs(allowed);
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/topic/all", true), channel))
                    .isInstanceOf(MessagingException.class);
            assertThatThrownBy(() -> interceptor.preSend(frame(StompCommand.SUBSCRIBE, null, "/user/queue/chat", false), channel))
                    .isInstanceOf(MessagingException.class)
                    .hasMessageContaining("авторизуватися");
        }

        @Test
        @DisplayName("інші кадри й повідомлення без STOMP-заголовків пропускаються")
        void passThrough() {
            Message<byte[]> send = frame(StompCommand.SEND, null, "/app/chat/1/send", true);
            assertThat(interceptor.preSend(send, channel)).isSameAs(send);
            Message<String> plain = new GenericMessage<>("ping");
            assertThat(interceptor.preSend(plain, channel)).isSameAs(plain);
        }
    }
}
