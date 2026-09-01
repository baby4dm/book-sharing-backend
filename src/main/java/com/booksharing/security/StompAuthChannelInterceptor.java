package com.booksharing.security;

import io.jsonwebtoken.JwtException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * HTTP-хендшейк WebSocket не завжди несе {@code Authorization}-заголовок
 * так само, як звичайний REST-запит (залежить від STOMP-клієнта в
 * браузері) - тому автентифікація відбувається не на хендшейку, а на
 * самому STOMP {@code CONNECT}-фреймі, де JS-клієнт явно передає токен
 * як native header. Це стандартний підхід для JWT + STOMP.
 */
@Component
@RequiredArgsConstructor
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new MessagingException("Відсутній токен авторизації для WebSocket-з'єднання");
            }
            String token = authHeader.substring(7);
            try {
                UUID userId = jwtService.extractUserId(token);
                // принципал WebSocket-сесії - лише userId (рядком); повний User
                // підвантажується в ChatWebSocketController за потреби
                accessor.setUser(new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
            } catch (JwtException ex) {
                throw new MessagingException("Токен недійсний або протермінований");
            }
        }

        return message;
    }
}