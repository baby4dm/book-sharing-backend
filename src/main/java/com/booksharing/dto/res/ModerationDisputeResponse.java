package com.booksharing.dto.res;

import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.DisputeStatus;
import com.booksharing.enums.ExchangeStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ModerationDisputeResponse(
        UUID id,
        UUID exchangeId,
        DisputeStatus status,
        String description,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt,
        String resolutionComment,
        String moderatorName,
        UUID filedById,
        String filedByName,
        boolean filedByOwner,
        String bookTitle,
        String bookAuthor,
        String bookCoverUrl,
        ExchangeStatus exchangeStatus,
        DeliveryMethod deliveryMethod,
        ExchangeParticipantResponse owner,
        ExchangeParticipantResponse reader) {
}
