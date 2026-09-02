package com.booksharing.security;

import com.booksharing.entity.User;
import com.booksharing.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Перевіряє заголовок {@code Authorization: Bearer <token>} на кожному
 * запиті до {@code /api/**}. Принципал у SecurityContext — повний
 * {@link User}, завантажений з БД, тому в контролерах можна писати
 * просто {@code @AuthenticationPrincipal User currentUser}.
 * <p>
 * MVP-варіант: користувач вантажиться з БД на кожен запит (без кешу).
 * Прийнятно для навчального обсягу навантаження; за потреби пізніше
 * можна додати кеш (Caffeine/Redis) без зміни контракту фільтра.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parseClaims(token);
                UUID userId = UUID.fromString(claims.getSubject());

                userRepository.findById(userId).ifPresent(user -> {
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                });
            } catch (JwtException | IllegalArgumentException ex) {
                // невалідний/протермінований токен - просто не автентифікуємо;
                // запит піде далі як анонімний і впаде на .authenticated() нижче,
                // якщо ендпоінт цього вимагає
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}