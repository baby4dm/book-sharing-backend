package com.booksharing.dto.res;

import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ModerationUserResponse(
        UUID id,
        String name,
        String email,
        String avatarUrl,
        UserRole role,
        UserStatus status,
        LocalDateTime restrictedUntil,
        BigDecimal ratingAvg,
        int booksGiven,
        int booksTaken,
        int booksOverdue,
        int booksDamaged,
        LocalDateTime createdAt) {
}
