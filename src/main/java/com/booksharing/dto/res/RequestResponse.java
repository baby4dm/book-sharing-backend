package com.booksharing.dto.res;

import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.RequestStatus;
import java.math.BigDecimal;
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
        Integer readingDays,
        DeliveryMethod preferredDeliveryMethod,
        String message,
        RequestStatus status,
        String rejectComment,
        LocalDateTime createdAt,
        LocalDateTime decidedAt,
        UUID exchangeId) {

    public RequestResponse withExchangeId(UUID exchangeId) {
        return new RequestResponse(
                id, listingId, listingBookTitle, requesterId, requesterName,
                requesterAvatarUrl, requesterRatingAvg, readingDays,
                preferredDeliveryMethod, message, status, rejectComment,
                createdAt, decidedAt, exchangeId);
    }
}