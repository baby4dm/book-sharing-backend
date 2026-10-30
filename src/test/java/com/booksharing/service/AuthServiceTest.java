package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.common.exception.InvalidCredentialsException;
import com.booksharing.dto.req.LoginRequest;
import com.booksharing.dto.req.RegisterRequest;
import com.booksharing.dto.res.AuthResponse;
import com.booksharing.entity.User;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.repository.UserRepository;
import com.booksharing.security.JwtService;
import com.booksharing.support.TestData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Реєстрація та вхід за email")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestData.user("Олена");
        user.setPasswordHash("hash");
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.generateToken(any(), any(), any())).thenReturn("jwt-token");
    }

    @Test
    @DisplayName("реєстрація зберігає хеш пароля й повертає токен")
    void register() {
        when(userRepository.existsByEmail("new@test.ua")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        AuthResponse response = authService.register(new RegisterRequest("new@test.ua", "secret123", "Новий"));

        assertThat(response.token()).isEqualTo("jwt-token");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded");
        assertThat(captor.getValue().getRole()).isEqualTo(UserRole.USER);
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        verify(jwtService).generateToken(any(), eq("new@test.ua"), eq(List.of("USER")));
    }

    @Test
    @DisplayName("повторна реєстрація з тим самим email відхиляється")
    void duplicateEmail() {
        when(userRepository.existsByEmail(user.getEmail())).thenReturn(true);
        assertThatThrownBy(() -> authService.register(new RegisterRequest(user.getEmail(), "secret123", "x")))
                .isInstanceOf(IllegalStateException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("вхід з правильним паролем")
    void login() {
        assertThat(authService.login(new LoginRequest(user.getEmail(), "secret123")).token()).isEqualTo("jwt-token");
    }

    @Test
    @DisplayName("невірний пароль або email")
    void wrongCredentials() {
        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "wrong")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Невірний email або пароль");
        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@test.ua", "secret123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    @DisplayName("акаунт Google без пароля")
    void googleAccount() {
        user.setPasswordHash(null);
        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "secret123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("через Google");
    }

    @Test
    @DisplayName("заблокований користувач не входить")
    void blocked() {
        user.setStatus(UserStatus.BLOCKED);
        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "secret123")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("заблоковано");
    }
}
