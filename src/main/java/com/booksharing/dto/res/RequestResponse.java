package com.booksharing.dto.res;

import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.RequestStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record RequestResponse(
        UUID id,
        UUID listingId,
        String listingBookTitle,
        UUID requesterId,
        String requesterName,
        String requesterAvatarUrl,
        BigDecimal requesterRatingAvg,
        LocalDate desiredDeadline,
        DeliveryMethod preferredDeliveryMethod,
        String message,
        RequestStatus status,
        String rejectComment,
        LocalDateTime createdAt,
        LocalDateTime decidedAt) {
}