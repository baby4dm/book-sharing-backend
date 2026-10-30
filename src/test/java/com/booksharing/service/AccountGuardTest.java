package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.common.exception.AccountRestrictedException;
import com.booksharing.entity.User;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Перевірка права створювати контент")
class AccountGuardTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountGuard accountGuard;

    private User user(UserRole role, UserStatus status, LocalDateTime restrictedUntil) {
        User user = User.builder().id(UUID.randomUUID()).name("Тест").role(role).build();
        user.setStatus(status);
        user.setRestrictedUntil(restrictedUntil);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        return user;
    }

    @Test
    @DisplayName("активний користувач проходить перевірку")
    void activeUserPasses() {
        User u = user(UserRole.USER, UserStatus.ACTIVE, null);
        assertThatCode(() -> accountGuard.requireCanCreateContent(u.getId())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("модератор не може створювати оголошення й заявки")
    void moderatorIsRejected() {
        User u = user(UserRole.MODERATOR, UserStatus.ACTIVE, null);
        assertThatThrownBy(() -> accountGuard.requireCanCreateContent(u.getId()))
                .isInstanceOf(AccountRestrictedException.class)
                .hasMessage(AccountGuard.STAFF_FORBIDDEN);
    }

    @Test
    @DisplayName("заблокований користувач отримує відмову")
    void blockedUserIsRejected() {
        User u = user(UserRole.USER, UserStatus.BLOCKED, null);
        assertThatThrownBy(() -> accountGuard.requireCanCreateContent(u.getId()))
                .isInstanceOf(AccountRestrictedException.class)
                .hasMessageContaining("заблоковано");
    }

    @Test
    @DisplayName("під час обмеження повідомляється дата його завершення")
    void activeRestrictionIsReported() {
        LocalDateTime until = LocalDateTime.of(2099, 1, 15, 12, 0);
        User u = user(UserRole.USER, UserStatus.RESTRICTED, until);
        assertThatThrownBy(() -> accountGuard.requireCanCreateContent(u.getId()))
                .isInstanceOf(AccountRestrictedException.class)
                .hasMessageContaining("15.01.2099");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("обмеження, строк якого минув, знімається автоматично")
    void expiredRestrictionIsLifted() {
        User u = user(UserRole.USER, UserStatus.RESTRICTED, LocalDateTime.now().minusHours(1));
        assertThatCode(() -> accountGuard.requireCanCreateContent(u.getId())).doesNotThrowAnyException();
        assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(u.getRestrictedUntil()).isNull();
        verify(userRepository).save(u);
    }
}
