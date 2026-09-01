package com.booksharing.model.request;

import com.booksharing.model.listing.DeliveryMethod;
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