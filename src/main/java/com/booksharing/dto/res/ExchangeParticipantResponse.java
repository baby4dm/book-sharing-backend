package com.booksharing.dto.res;

import java.math.BigDecimal;
import java.util.UUID;

public record ExchangeParticipantResponse(
        UUID id,
        String name,
        String avatarUrl,
        BigDecimal ratingAvg,
        int booksGiven,
        int booksTaken,
        int booksOverdue) {
}