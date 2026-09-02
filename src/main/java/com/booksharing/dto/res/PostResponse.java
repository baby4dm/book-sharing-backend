package com.booksharing.dto.res;

import java.time.LocalDateTime;
import java.util.UUID;

public record PostResponse(
        UUID id,
        UUID authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        String photoUrl,
        long commentsCount,
        LocalDateTime createdAt) {
}