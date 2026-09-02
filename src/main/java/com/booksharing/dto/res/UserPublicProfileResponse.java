package com.booksharing.dto.res;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Публічний профіль користувача — те, що бачать інші (GET /api/users/{id}).
 * Свідомо без email, role, status, restrictedUntil — це вже дані для
 * {@link UserMeResponse}, а не для сторонніх переглядачів профілю.
 */
public record UserPublicProfileResponse(
        UUID id,
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
        LocalDateTime createdAt) {
}