package com.booksharing.dto.res;

import com.booksharing.enums.ShipmentCarrier;
import com.booksharing.enums.ShipmentDirection;
import com.booksharing.enums.ShipmentStatus;

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