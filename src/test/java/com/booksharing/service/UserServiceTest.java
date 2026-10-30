package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.UpdateUserProfileRequest;
import com.booksharing.dto.res.UserMeResponse;
import com.booksharing.dto.res.UserPublicProfileResponse;
import com.booksharing.entity.User;
import com.booksharing.enums.SettlementType;
import com.booksharing.enums.UserRole;
import com.booksharing.mapper.UserMapper;
import com.booksharing.repository.UserRepository;
import com.booksharing.support.TestData;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Профілі користувачів")
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserStatsService userStatsService;
    @Spy private UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestData.user("Олена");
        user.setSettlementName("Рівне");
        user.setBio("Люблю детективи");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userStatsService.statsFor(any())).thenReturn(new UserStatsService.UserExchangeStats(4, 3, 2, 1, 0));
    }

    @Test
    @DisplayName("публічний профіль показує статистику з обмінів")
    void publicProfile() {
        UserPublicProfileResponse profile = userService.getPublicProfile(user.getId());
        assertThat(profile.name()).isEqualTo("Олена");
        assertThat(profile.settlementName()).isEqualTo("Рівне");
        assertThat(profile.booksGiven()).isEqualTo(4);
        assertThat(profile.booksTaken()).isEqualTo(3);
        assertThat(profile.booksReturnedOnTime()).isEqualTo(2);
        assertThat(profile.booksOverdue()).isEqualTo(1);
        assertThat(profile.booksDamaged()).isZero();
    }

    @Test
    @DisplayName("профіль модератора не показує місто, рейтинг і статистику")
    void staffProfile() {
        User moderator = TestData.user("Модератор", UserRole.MODERATOR);
        moderator.setSettlementName("Київ");
        when(userRepository.findById(moderator.getId())).thenReturn(Optional.of(moderator));

        UserPublicProfileResponse profile = userService.getPublicProfile(moderator.getId());

        assertThat(profile.settlementName()).isNull();
        assertThat(profile.ratingAvg()).isNull();
        assertThat(profile.booksGiven()).isNull();
        assertThat(profile.role()).isEqualTo(UserRole.MODERATOR);
    }

    @Test
    @DisplayName("неіснуючий профіль")
    void notFound() {
        assertThatThrownBy(() -> userService.getPublicProfile(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("власний профіль містить email і статус")
    void myProfile() {
        UserMeResponse me = userService.getMyProfile(user);
        assertThat(me.email()).isEqualTo(user.getEmail());
        assertThat(me.status()).isEqualTo(user.getStatus());
        assertThat(me.booksGiven()).isEqualTo(4);
    }

    @Test
    @DisplayName("оновлення змінює лише передані поля")
    void update() {
        UserMeResponse updated = userService.updateMyProfile(user.getId(),
                new UpdateUserProfileRequest("Олена Петрівна", null, SettlementType.CITY, "Рівненська", null, null));

        assertThat(updated.name()).isEqualTo("Олена Петрівна");
        assertThat(updated.settlementType()).isEqualTo(SettlementType.CITY);
        assertThat(updated.settlementName()).isEqualTo("Рівне");
        assertThat(updated.bio()).isEqualTo("Люблю детективи");
        verify(userRepository).save(user);
    }
}
