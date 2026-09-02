package com.booksharing.dto.res;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommentResponse(
        UUID id,
        UUID postId,
        UUID authorId,
        String authorName,
        String authorAvatarUrl,
        String content,
        LocalDateTime createdAt) {
}