package com.booksharing.model.user;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Власний профіль (GET /api/users/me) — усе те саме, що й публічний
 * профіль, плюс приватні поля, які бачить лише сам користувач.
 */
public record UserMeResponse(
        UUID id,
        String email,
        String name,
        String avatarUrl,
        String city,
        String bio,
        BigDecimal ratingAvg,
        Integer booksTaken,
        Integer booksReturnedOnTime,
        Integer booksOverdue,
        Integer booksDamaged,
        Integer booksGiven,
        UserRole role,
        UserStatus status,
        LocalDateTime restrictedUntil,
        LocalDateTime createdAt) {
}