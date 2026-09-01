package com.booksharing.model.exchange;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShipmentInfoResponse(
        UUID id,
        ShipmentDirection direction,
        String recipientName,
        String recipientPhone,
        ShipmentCarrier carrier,
        String city,
        String branchNumber,
        String waybillPhotoUrl,
        ShipmentStatus status,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt) {
}