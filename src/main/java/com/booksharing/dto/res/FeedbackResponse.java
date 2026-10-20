package com.booksharing.dto.res;

import com.booksharing.enums.FeedbackStatus;
import com.booksharing.enums.FeedbackTopic;
import java.time.LocalDateTime;
import java.util.UUID;

public record FeedbackResponse(
        UUID id,
        FeedbackTopic topic,
        String email,
        String message,
        String link,
        String screenshotUrl,
        UUID userId,
        String userName,
        String userAvatarUrl,
        FeedbackStatus status,
        String moderatorNote,
        String handledByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}