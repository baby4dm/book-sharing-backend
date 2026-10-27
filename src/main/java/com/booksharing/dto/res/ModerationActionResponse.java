package com.booksharing.dto.res;

import com.booksharing.enums.ModerationActionType;
import com.booksharing.enums.ModerationTargetType;
import java.time.LocalDateTime;
import java.util.UUID;

public record ModerationActionResponse(
        UUID id,
        ModerationActionType action,
        ModerationTargetType targetType,
        UUID targetId,
        UUID targetUserId,
        String targetUserName,
        UUID moderatorId,
        String moderatorName,
        String reason,
        String details,
        LocalDateTime createdAt) {
}
