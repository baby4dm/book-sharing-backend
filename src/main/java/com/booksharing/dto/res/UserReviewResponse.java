package com.booksharing.dto.res;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserReviewResponse(
        UUID id,
        UUID exchangeId,
        String bookTitle,
        UUID authorId,
        String authorName,
        String authorAvatarUrl,
        String authorRole,
        int rating,
        String comment,
        LocalDateTime createdAt) {
}