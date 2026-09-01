package com.booksharing.model.exchange;

import com.booksharing.model.listing.DeliveryMethod;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ExchangeResponse(
        UUID id,
        UUID listingId,
        String bookTitle,
        UUID ownerId,
        String ownerName,
        UUID readerId,
        String readerName,
        DeliveryMethod deliveryMethod,
        LocalDate deadline,
        LocalDate extendedDeadline,
        ExchangeStatus status,
        List<ExchangePhotoResponse> handoverPhotos,
        List<ExchangePhotoResponse> returnPhotos,
        List<ShipmentInfoResponse> shipments,
        List<DeadlineExtensionResponse> extensionRequests,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
}