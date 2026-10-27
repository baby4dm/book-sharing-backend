package com.booksharing.service;

import com.booksharing.common.exception.AccountRestrictedException;
import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.entity.User;
import com.booksharing.enums.UserStatus;
import com.booksharing.repository.UserRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class AccountGuard {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final UserRepository userRepository;

    @Transactional
    public void requireCanCreateContent(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено: " + userId));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountRestrictedException("Обліковий запис заблоковано модератором");
        }
        if (user.getStatus() != UserStatus.RESTRICTED) {
            return;
        }

        LocalDateTime until = user.getRestrictedUntil();
        if (until != null && !until.isAfter(LocalDateTime.now())) {
            user.setStatus(UserStatus.ACTIVE);
            user.setRestrictedUntil(null);
            userRepository.save(user);
            return;
        }

        throw new AccountRestrictedException(until == null
                ? "Ваш обліковий запис обмежено. Ви можете завершити поточні обміни, але не створювати нові"
                : "Ваш обліковий запис обмежено до " + until.format(DATE)
                        + ". До того часу можна завершувати поточні обміни, але не створювати нові");
    }
}
