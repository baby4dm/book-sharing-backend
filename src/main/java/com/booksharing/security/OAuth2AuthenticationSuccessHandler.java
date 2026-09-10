package com.booksharing.security;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.entity.User;
import com.booksharing.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Спрацьовує і для Google OIDC-логіну (принципал - OidcUser, який теж
 * реалізує OAuth2User), і потенційно для інших OAuth2-провайдерів у
 * майбутньому. Шукає User за email напряму з БД - жоден кастомний
 * "localUserId"-атрибут більше не потрібен (OidcUser не дає чистого
 * способу такий атрибут прикріпити, на відміну від DefaultOAuth2User).
 */
@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final String frontendBaseUrl;
    private final String oauth2RedirectPath;

    public OAuth2AuthenticationSuccessHandler(
            JwtService jwtService,
            UserRepository userRepository,
            @Value("${app.frontend.base-url}") String frontendBaseUrl,
            @Value("${app.frontend.oauth2-redirect-path}") String oauth2RedirectPath) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.frontendBaseUrl = frontendBaseUrl;
        this.oauth2RedirectPath = oauth2RedirectPath;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Користувача не знайдено після OAuth2-логіну: " + email));

        String token = jwtService.generateToken(
                user.getId(), user.getEmail(), List.of("ROLE_" + user.getRole().name()));

        String redirectUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path(oauth2RedirectPath)
                .queryParam("token", token)
                .build()
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}