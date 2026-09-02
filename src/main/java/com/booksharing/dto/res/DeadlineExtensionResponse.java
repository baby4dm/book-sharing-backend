package com.booksharing.dto.res;

import com.booksharing.enums.ExtensionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DeadlineExtensionResponse(
        UUID id,
        LocalDate requestedNewDeadline,
        ExtensionStatus status,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime decidedAt) {
}