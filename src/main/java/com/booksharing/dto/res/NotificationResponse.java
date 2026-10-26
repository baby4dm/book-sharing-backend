package com.booksharing.dto.res;

import com.booksharing.enums.NotificationType;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        UUID referenceId,
        String message,
        boolean read,
        LocalDateTime createdAt,
        String link) {
}