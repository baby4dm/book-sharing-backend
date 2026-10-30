package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.booksharing.entity.Exchange;
import com.booksharing.entity.User;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.repository.ExchangeRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Статистика користувача з історії обмінів")
class UserStatsServiceTest {

    @Mock
    private ExchangeRepository exchangeRepository;

    @InjectMocks
    private UserStatsService userStatsService;

    private static Exchange completed(LocalDate deadline, int pausedDays, LocalDate returnedOn) {
        return Exchange.builder()
                .status(ExchangeStatus.COMPLETED)
                .deadline(deadline)
                .readingPausedDays(pausedDays)
                .returnedAt(returnedOn.atTime(18, 0))
                .completedAt(returnedOn.plusDays(2).atStartOfDay())
                .build();
    }

    @Test
    @DisplayName("рахує віддані, отримані, вчасні та прострочені повернення")
    void countsExchanges() {
        User user = User.builder().id(UUID.randomUUID()).name("Читач").booksDamaged(1).build();
        LocalDate deadline = LocalDate.of(2026, 9, 10);

        when(exchangeRepository.countByOwnerIdAndStatus(user.getId(), ExchangeStatus.COMPLETED)).thenReturn(5L);
        when(exchangeRepository.findByReaderIdAndStatus(user.getId(), ExchangeStatus.COMPLETED)).thenReturn(List.of(
                completed(deadline, 0, deadline),                // в останній день
                completed(deadline, 0, deadline.minusDays(4)),   // раніше строку
                completed(deadline, 3, deadline.plusDays(2)),    // пізніше, але в межах днів пересилання
                completed(deadline, 0, deadline.plusDays(1))));  // прострочено
        when(exchangeRepository.countByReaderIdAndStatus(user.getId(), ExchangeStatus.OVERDUE)).thenReturn(1L);

        UserStatsService.UserExchangeStats stats = userStatsService.statsFor(user);

        assertThat(stats.booksGiven()).isEqualTo(5);
        assertThat(stats.booksTaken()).isEqualTo(4);
        assertThat(stats.booksReturnedOnTime()).isEqualTo(3);
        assertThat(stats.booksOverdue()).isEqualTo(2);
        assertThat(stats.booksDamaged()).isEqualTo(1);
    }

    @Test
    @DisplayName("без дати повернення використовується дата завершення обміну")
    void fallsBackToCompletedAt() {
        User user = User.builder().id(UUID.randomUUID()).name("Читач").build();
        Exchange late = Exchange.builder()
                .status(ExchangeStatus.COMPLETED)
                .deadline(LocalDate.of(2026, 9, 10))
                .completedAt(LocalDateTime.of(2026, 9, 12, 10, 0))
                .build();
        when(exchangeRepository.findByReaderIdAndStatus(user.getId(), ExchangeStatus.COMPLETED)).thenReturn(List.of(late));

        UserStatsService.UserExchangeStats stats = userStatsService.statsFor(user);

        assertThat(stats.booksReturnedOnTime()).isZero();
        assertThat(stats.booksOverdue()).isEqualTo(1);
    }
}
