package com.booksharing.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Спрацьовує одразу після того, як {@link CustomOAuth2UserService} успішно
 * знайшов/створив {@code User} у БД. Тут видаємо власний JWT і
 * перенаправляємо браузер на фронтенд-колбек з токеном у query-параметрі.
 * <p>
 * Фронтенд (наприклад, на {@code /oauth-callback}) забирає {@code token}
 * з URL, зберігає його (пам'ять/localStorage) і надалі шле в заголовку
 * {@code Authorization: Bearer <token>} для всіх запитів до {@code /api/**}.
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final String frontendBaseUrl;
    private final String oauth2RedirectPath;

    public OAuth2AuthenticationSuccessHandler(
            JwtService jwtService,
            @Value("${app.frontend.base-url}") String frontendBaseUrl,
            @Value("${app.frontend.oauth2-redirect-path}") String oauth2RedirectPath) {
        this.jwtService = jwtService;
        this.frontendBaseUrl = frontendBaseUrl;
        this.oauth2RedirectPath = oauth2RedirectPath;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        UUID userId = UUID.fromString(oauth2User.getAttribute("localUserId"));
        String email = oauth2User.getAttribute("email");
        String role = oauth2User.getAttribute("localUserRole");

        String token = jwtService.generateToken(userId, email, List.of(role));

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path(oauth2RedirectPath)
                .queryParam("token", token)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}