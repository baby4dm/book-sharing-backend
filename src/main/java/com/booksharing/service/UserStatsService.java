package com.booksharing.service;

import com.booksharing.entity.Exchange;
import com.booksharing.entity.User;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.repository.ExchangeRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserStatsService {

    private final ExchangeRepository exchangeRepository;

    public record UserExchangeStats(
            int booksGiven,
            int booksTaken,
            int booksReturnedOnTime,
            int booksOverdue,
            int booksDamaged) {
    }

    @Transactional(readOnly = true)
    public UserExchangeStats statsFor(User user) {
        UUID userId = user.getId();

        int given = (int) exchangeRepository.countByOwnerIdAndStatus(userId, ExchangeStatus.COMPLETED);

        List<Exchange> completedAsReader =
                exchangeRepository.findByReaderIdAndStatus(userId, ExchangeStatus.COMPLETED);
        int taken = completedAsReader.size();

        int onTime = 0;
        int lateReturned = 0;
        for (Exchange exchange : completedAsReader) {
            LocalDate dueDate = exchange.getDueDate();
            LocalDate returnedOn = exchange.getReturnedAt() != null
                    ? exchange.getReturnedAt().toLocalDate()
                    : exchange.getCompletedAt() != null ? exchange.getCompletedAt().toLocalDate() : null;
            if (dueDate == null || returnedOn == null || !returnedOn.isAfter(dueDate)) {
                onTime++;
            } else {
                lateReturned++;
            }
        }
        int overdueNow = (int) exchangeRepository.countByReaderIdAndStatus(userId, ExchangeStatus.OVERDUE);

        int damaged = user.getBooksDamaged() == null ? 0 : user.getBooksDamaged();
        return new UserExchangeStats(given, taken, onTime, lateReturned + overdueNow, damaged);
    }
}
