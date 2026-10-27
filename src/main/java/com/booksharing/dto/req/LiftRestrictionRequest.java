package com.booksharing.dto.req;

import jakarta.validation.constraints.Size;

public record LiftRestrictionRequest(
        @Size(max = 1000, message = "Коментар занадто довгий")
        String reason) {
}
