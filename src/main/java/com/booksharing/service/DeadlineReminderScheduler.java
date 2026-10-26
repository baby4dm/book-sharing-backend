package com.booksharing.service;

import com.booksharing.entity.Exchange;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.repository.DeadlineReminderRepository;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
public class DeadlineReminderScheduler {

    private static final int SOON_DAYS = 2;
    private static final int KEEP_READ_NOTIFICATIONS_DAYS = 90;
    private static final List<ExchangeStatus> READING = List.of(ExchangeStatus.IN_READING, ExchangeStatus.OVERDUE);

    private final DeadlineReminderRepository reminderRepository;
    private final ExchangeNotifier exchangeNotifier;
    private final NotificationService notificationService;
    private final TransactionTemplate tx;

    public DeadlineReminderScheduler(
            DeadlineReminderRepository reminderRepository,
            ExchangeNotifier exchangeNotifier,
            NotificationService notificationService,
            PlatformTransactionManager transactionManager) {
        this.reminderRepository = reminderRepository;
        this.exchangeNotifier = exchangeNotifier;
        this.notificationService = notificationService;
        this.tx = new TransactionTemplate(transactionManager);
    }

    @Scheduled(cron = "0 5 9-21 * * *", zone = "Europe/Kyiv")
    public void sendDeadlineReminders() {
        List<Exchange> exchanges = tx.execute(status -> reminderRepository.findActiveReadings(READING));
        if (exchanges == null || exchanges.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        int sent = 0;
        for (Exchange exchange : exchanges) {
            try {
                Boolean result = tx.execute(status -> remind(exchange, today));
                if (Boolean.TRUE.equals(result)) {
                    sent++;
                }
            } catch (RuntimeException ex) {
                log.warn("Не вдалося надіслати нагадування для обміну {}", exchange.getId(), ex);
            }
        }
        if (sent > 0) {
            log.info("Надіслано нагадувань про термін читання: {}", sent);
        }
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "Europe/Kyiv")
    public void cleanupOldNotifications() {
        int deleted = notificationService.deleteReadOlderThan(KEEP_READ_NOTIFICATIONS_DAYS);
        if (deleted > 0) {
            log.info("Видалено старих прочитаних сповіщень: {}", deleted);
        }
    }

    private boolean remind(Exchange exchange, LocalDate today) {
        LocalDate due = exchange.getDueDate();
        if (due == null) {
            return false;
        }
        long daysLeft = ChronoUnit.DAYS.between(today, due);

        if (daysLeft < 0) {
            if (reminderRepository.markSent(exchange.getId(), "OVERDUE", due) == 1) {
                exchangeNotifier.deadlineOverdue(exchange);
                return true;
            }
        } else if (daysLeft == 0) {
            if (reminderRepository.markSent(exchange.getId(), "LAST_DAY", due) == 1) {
                exchangeNotifier.deadlineLastDay(exchange);
                return true;
            }
        } else if (daysLeft <= SOON_DAYS) {
            if (reminderRepository.markSent(exchange.getId(), "SOON", due) == 1) {
                exchangeNotifier.deadlineSoon(exchange, daysLeft);
                return true;
            }
        }
        return false;
    }
}