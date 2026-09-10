package com.booksharing.security;

import com.booksharing.entity.User;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Google-логін іде через scope "openid" -> Spring Security обирає OIDC-гілку
 * (ID Token), а НЕ звичайний OAuth2UserService, налаштований через
 * userInfoEndpoint().userService(...) в SecurityConfig. Тому саме цей
 * сервіс - той, що реально спрацьовує для Google, а не CustomOAuth2UserService.
 * <p>
 * На відміну від нього, тут НЕ намагаємось "прикріпити" localUserId як
 * додатковий атрибут - у DefaultOidcUser немає для цього чистого способу
 * (claims там похідні від ID Token/UserInfo, не довільна мапа). Замість
 * цього {@link OAuth2AuthenticationSuccessHandler} сам шукає User за email
 * після успішної автентифікації.
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String googleId = oidcUser.getSubject();
        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();
        String avatarUrl = oidcUser.getPicture();

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Google-акаунт не надав email");
        }

        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email))
                .map(existing -> {
                    existing.setGoogleId(googleId);
                    existing.setName(name);
                    existing.setAvatarUrl(avatarUrl);
                    return existing;
                })
                .orElseGet(() -> User.builder()
                        .googleId(googleId)
                        .email(email)
                        .name(name)
                        .avatarUrl(avatarUrl)
                        .role(UserRole.USER)
                        .status(UserStatus.ACTIVE)
                        .build());

        userRepository.save(user);

        return new DefaultOidcUser(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                oidcUser.getIdToken(),
                oidcUser.getUserInfo());
    }
}