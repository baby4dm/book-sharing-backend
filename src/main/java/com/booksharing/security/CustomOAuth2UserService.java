package com.booksharing.security;

import com.booksharing.model.user.User;
import com.booksharing.model.user.UserRepository;
import com.booksharing.model.user.UserRole;
import com.booksharing.model.user.UserStatus;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Після того як Spring Security забрав профіль користувача з Google,
 * тут ми знаходимо або створюємо відповідний {@link User} у нашій БД.
 * <p>
 * {@code oauth2User.getName()} повертає значення claim'у {@code sub} —
 * стабільний ідентифікатор користувача в Google (те, що ми зберігаємо
 * як {@code google_id}). Це працює "з коробки", бо Spring Boot реєструє
 * Google як провайдера з {@code user-name-attribute: sub} за замовчуванням
 * (через {@code CommonOAuth2Provider.GOOGLE}).
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        String googleId = oauth2User.getName(); // claim "sub"
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");
        String avatarUrl = (String) attributes.get("picture");

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

        user = userRepository.save(user);

        // додаємо локальний id в атрибути, щоб success-handler міг видати
        // JWT з правильним subject, не звертаючись повторно до БД
        Map<String, Object> enrichedAttributes = new HashMap<>(attributes);
        enrichedAttributes.put("localUserId", user.getId().toString());
        enrichedAttributes.put("localUserRole", user.getRole().name());

        return new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
                enrichedAttributes,
                "sub");
    }
}