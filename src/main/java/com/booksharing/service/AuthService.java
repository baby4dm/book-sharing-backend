package com.booksharing.service;

import com.booksharing.common.exception.InvalidCredentialsException;
import com.booksharing.dto.req.LoginRequest;
import com.booksharing.dto.req.RegisterRequest;
import com.booksharing.dto.res.AuthResponse;
import com.booksharing.entity.User;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.repository.UserRepository;
import com.booksharing.security.JwtService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Локальна реєстрація/логін email+пароль — паралельний шлях до Google
 * OAuth2 ({@link com.booksharing.security.CustomOAuth2UserService}).
 * Обидва шляхи видають той самий JWT через {@link JwtService}, тому
 * решті застосунку (JwtAuthenticationFilter, контролери) байдуже, як
 * саме користувач увійшов.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalStateException("Користувач з таким email уже зареєстрований");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), List.of(user.getRole().name()));
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Невірний email або пароль"));

        if (user.getPasswordHash() == null) {
            // акаунт створено через Google - локального пароля немає
            throw new InvalidCredentialsException(
                    "Цей акаунт зареєстрований через Google. Увійдіть через Google");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Невірний email або пароль");
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new InvalidCredentialsException("Обліковий запис заблоковано");
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail(), List.of(user.getRole().name()));
        return new AuthResponse(token);
    }
}