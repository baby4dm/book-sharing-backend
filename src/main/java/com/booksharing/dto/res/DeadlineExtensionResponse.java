package com.booksharing.dto.res;

import com.booksharing.enums.ExtensionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DeadlineExtensionResponse(
        UUID id,
        Integer extraDays,
        LocalDate requestedNewDeadline,
        ExtensionStatus status,
        String comment,
        String decisionComment,
        LocalDateTime createdAt,
        LocalDateTime decidedAt) {
}