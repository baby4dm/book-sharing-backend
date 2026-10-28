package com.booksharing.dto.res;

import com.booksharing.enums.SettlementType;

import com.booksharing.enums.UserRole;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record UserPublicProfileResponse(
        UUID id,
        String name,
        String avatarUrl,
        SettlementType settlementType,
        String region,
        String settlementName,
        String bio,
        BigDecimal ratingAvg,
        Integer booksTaken,
        Integer booksReturnedOnTime,
        Integer booksOverdue,
        Integer booksDamaged,
        Integer booksGiven,
        LocalDateTime createdAt,
        UserRole role) {
}