package com.booksharing.dto.res;

import com.booksharing.enums.DisputeStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DisputeResponse(
        UUID id,
        UUID exchangeId,
        UUID filedByUserId,
        String filedByName,
        String description,
        DisputeStatus status,
        UUID moderatorId,
        String moderatorName,
        String resolutionComment,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt) {
}