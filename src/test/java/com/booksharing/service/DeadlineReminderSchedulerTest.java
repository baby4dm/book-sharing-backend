package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.booksharing.entity.Exchange;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.repository.DeadlineReminderRepository;
import com.booksharing.support.TestData;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Нагадування про термін читання")
class DeadlineReminderSchedulerTest {

    @Mock private DeadlineReminderRepository reminderRepository;
    @Mock private ExchangeNotifier exchangeNotifier;
    @Mock private NotificationService notificationService;
    @Mock private PlatformTransactionManager transactionManager;

    private DeadlineReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(reminderRepository.markSent(any(), anyString(), any())).thenReturn(1);
        scheduler = new DeadlineReminderScheduler(reminderRepository, exchangeNotifier, notificationService, transactionManager);
    }

    private Exchange readingUntil(LocalDate due) {
        Exchange e = TestData.exchange(TestData.user("Власник"), TestData.user("Читач"),
                DeliveryMethod.PICKUP, ExchangeStatus.IN_READING);
        e.setDeadline(due);
        return e;
    }

    @Test
    @DisplayName("без активних читань нічого не надсилається")
    void nothingToDo() {
        when(reminderRepository.findActiveReadings(any())).thenReturn(List.of());
        scheduler.sendDeadlineReminders();
        verifyNoInteractions(exchangeNotifier);
    }

    @Test
    @DisplayName("за два дні, в останній день і після терміну надсилаються різні нагадування")
    void remindsByDaysLeft() {
        LocalDate today = LocalDate.now();
        Exchange soon = readingUntil(today.plusDays(2));
        Exchange lastDay = readingUntil(today);
        Exchange overdue = readingUntil(today.minusDays(1));
        Exchange farAway = readingUntil(today.plusDays(10));
        Exchange noDeadline = readingUntil(null);
        when(reminderRepository.findActiveReadings(any())).thenReturn(List.of(soon, lastDay, overdue, farAway, noDeadline));

        scheduler.sendDeadlineReminders();

        verify(exchangeNotifier).deadlineSoon(soon, 2);
        verify(exchangeNotifier).deadlineLastDay(lastDay);
        verify(exchangeNotifier).deadlineOverdue(overdue);
        verify(reminderRepository).markSent(eq(soon.getId()), eq("SOON"), eq(soon.getDueDate()));
        verify(reminderRepository).markSent(eq(lastDay.getId()), eq("LAST_DAY"), eq(today));
        verify(reminderRepository).markSent(eq(overdue.getId()), eq("OVERDUE"), eq(overdue.getDueDate()));
        verify(reminderRepository, never()).markSent(eq(farAway.getId()), anyString(), any());
    }

    @Test
    @DisplayName("вже надіслане нагадування не дублюється")
    void alreadySent() {
        when(reminderRepository.markSent(any(), anyString(), any())).thenReturn(0);
        LocalDate today = LocalDate.now();
        when(reminderRepository.findActiveReadings(any())).thenReturn(List.of(
                readingUntil(today.plusDays(1)), readingUntil(today), readingUntil(today.minusDays(3))));

        scheduler.sendDeadlineReminders();

        verify(exchangeNotifier, never()).deadlineSoon(any(), anyLong());
        verify(exchangeNotifier, never()).deadlineLastDay(any());
        verify(exchangeNotifier, never()).deadlineOverdue(any());
    }

    @Test
    @DisplayName("помилка в одному обміні не зупиняє решту")
    void failureIsIsolated() {
        LocalDate today = LocalDate.now();
        Exchange broken = readingUntil(today);
        Exchange fine = readingUntil(today.minusDays(1));
        when(reminderRepository.findActiveReadings(any())).thenReturn(List.of(broken, fine));
        doThrow(new IllegalStateException("збій")).when(exchangeNotifier).deadlineLastDay(broken);

        assertThatCode(() -> scheduler.sendDeadlineReminders()).doesNotThrowAnyException();
        verify(exchangeNotifier).deadlineOverdue(fine);
    }

    @Test
    @DisplayName("очищення старих прочитаних сповіщень")
    void cleanup() {
        when(notificationService.deleteReadOlderThan(anyInt())).thenReturn(3).thenReturn(0);
        scheduler.cleanupOldNotifications();
        scheduler.cleanupOldNotifications();
        verify(notificationService, org.mockito.Mockito.times(2)).deleteReadOlderThan(90);
    }
}
