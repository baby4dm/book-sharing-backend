package com.booksharing.dto.res;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExchangeReviewResponse(
        UUID id,
        UUID authorId,
        String authorName,
        String authorAvatarUrl,
        UUID targetId,
        int rating,
        String comment,
        LocalDateTime createdAt) {
}