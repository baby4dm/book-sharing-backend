package com.booksharing.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.entity.User;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.repository.UserRepository;
import com.booksharing.support.TestData;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Вхід через Google")
class OAuth2LoginTest {

    private static final String FRONTEND = "http://localhost:5173";

    @Nested
    @DisplayName("перенаправлення після входу")
    class SuccessHandler {

        private final JwtService jwtService = mock(JwtService.class);
        private final UserRepository userRepository = mock(UserRepository.class);
        private final OAuth2AuthenticationSuccessHandler handler =
                new OAuth2AuthenticationSuccessHandler(jwtService, userRepository, FRONTEND, "/oauth2/callback");

        private TestingAuthenticationToken authentication(String email) {
            DefaultOAuth2User principal = new DefaultOAuth2User(
                    List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("sub", "g-1", "email", email), "sub");
            return new TestingAuthenticationToken(principal, null);
        }

        @Test
        @DisplayName("користувач отримує JWT у посиланні на фронтенд")
        void redirectsWithToken() throws Exception {
            User user = TestData.user("Олена");
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user.getId(), user.getEmail(), List.of("ROLE_USER"))).thenReturn("jwt");
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication(user.getEmail()));

            assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND + "/oauth2/callback?token=jwt");
        }

        @Test
        @DisplayName("заблокований користувач повертається на сторінку входу")
        void blocked() throws Exception {
            User user = TestData.user("Заблокований");
            user.setStatus(UserStatus.BLOCKED);
            when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
            MockHttpServletResponse response = new MockHttpServletResponse();

            handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication(user.getEmail()));

            assertThat(response.getRedirectedUrl()).isEqualTo(FRONTEND + "/login?error=blocked");
            verify(jwtService, never()).generateToken(any(), any(), any());
        }

        @Test
        @DisplayName("невідомий email")
        void unknownUser() {
            assertThatThrownBy(() -> handler.onAuthenticationSuccess(new MockHttpServletRequest(),
                    new MockHttpServletResponse(), authentication("nobody@test.ua")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("дані користувача з ID-токена Google")
    class Oidc {

        private final UserRepository userRepository = mock(UserRepository.class);
        private final CustomOidcUserService service = new CustomOidcUserService(userRepository);

        private OidcUserRequest request(String email) {
            ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                    .clientId("client")
                    .clientSecret("secret")
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                    .scope("openid", "email", "profile")
                    .authorizationUri("https://accounts.example.com/auth")
                    .tokenUri("https://accounts.example.com/token")
                    .userNameAttributeName("sub")
                    .build();
            Instant now = Instant.now();
            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER, "access", now, now.plusSeconds(60));
            OidcIdToken.Builder idToken = OidcIdToken.withTokenValue("id-token")
                    .subject("google-123")
                    .claim("name", "Олена Коваль")
                    .claim("picture", "https://photo/1.jpg")
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(60));
            if (email != null) {
                idToken.claim("email", email);
            }
            return new OidcUserRequest(registration, accessToken, idToken.build());
        }

        @Test
        @DisplayName("новий користувач створюється з роллю USER")
        void createsUser() {
            when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("olena@test.ua")).thenReturn(Optional.empty());

            OidcUser result = service.loadUser(request("olena@test.ua"));

            ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(saved.capture());
            assertThat(saved.getValue().getGoogleId()).isEqualTo("google-123");
            assertThat(saved.getValue().getName()).isEqualTo("Олена Коваль");
            assertThat(saved.getValue().getRole()).isEqualTo(UserRole.USER);
            assertThat(result.getEmail()).isEqualTo("olena@test.ua");
            assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("наявний за email користувач прив'язується до Google")
        void linksByEmail() {
            User existing = TestData.user("Стара назва", UserRole.MODERATOR);
            when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.empty());
            when(userRepository.findByEmail("olena@test.ua")).thenReturn(Optional.of(existing));

            OidcUser result = service.loadUser(request("olena@test.ua"));

            assertThat(existing.getGoogleId()).isEqualTo("google-123");
            assertThat(existing.getName()).isEqualTo("Олена Коваль");
            assertThat(existing.getAvatarUrl()).isEqualTo("https://photo/1.jpg");
            verify(userRepository).save(existing);
            assertThat(result.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_MODERATOR");
        }

        @Test
        @DisplayName("пошук спершу за Google id")
        void findsByGoogleId() {
            User existing = TestData.user("Олена");
            when(userRepository.findByGoogleId("google-123")).thenReturn(Optional.of(existing));
            service.loadUser(request("olena@test.ua"));
            verify(userRepository, never()).findByEmail(eq("olena@test.ua"));
        }

        @Test
        @DisplayName("без email вхід неможливий")
        void noEmail() {
            assertThatThrownBy(() -> service.loadUser(request(null)))
                    .isInstanceOf(OAuth2AuthenticationException.class);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("налаштування безпеки")
    class Config {

        @Test
        @DisplayName("CORS дозволяє лише фронтенд і передачу облікових даних")
        void cors() {
            SecurityConfig config = new SecurityConfig(null, null, null, null);
            ReflectionTestUtils.setField(config, "frontendBaseUrl", FRONTEND);

            CorsConfigurationSource source = config.corsConfigurationSource();
            CorsConfiguration cors = source.getCorsConfiguration(new MockHttpServletRequest("GET", "/api/listings"));

            assertThat(cors.getAllowedOrigins()).containsExactly(FRONTEND);
            assertThat(cors.getAllowCredentials()).isTrue();
            assertThat(cors.getAllowedMethods()).contains("PATCH", "DELETE");
        }

        @Test
        @DisplayName("паролі хешуються BCrypt")
        void passwordEncoder() {
            PasswordEncoder encoder = new SecurityConfig(null, null, null, null).passwordEncoder();
            String hash = encoder.encode("secret123");
            assertThat(hash).startsWith("$2");
            assertThat(encoder.matches("secret123", hash)).isTrue();
        }
    }
}
