package com.booksharing.dto.res;

import com.booksharing.enums.DisputeStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExchangeDisputeResponse(
        UUID id,
        UUID filedByUserId,
        String description,
        DisputeStatus status,
        String resolutionComment,
        String moderatorName,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt) {
}