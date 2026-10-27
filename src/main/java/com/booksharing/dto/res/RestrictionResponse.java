package com.booksharing.dto.res;

import com.booksharing.enums.RestrictionType;
import java.time.LocalDateTime;
import java.util.UUID;

public record RestrictionResponse(
        UUID id,
        RestrictionType type,
        String reason,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        LocalDateTime liftedAt,
        String createdByName,
        String liftedByName,
        UUID disputeId,
        boolean active) {
}
