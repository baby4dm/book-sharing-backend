package com.booksharing.dto.req;

import jakarta.validation.constraints.Size;

public record DecideExtensionRequest(
        @Size(max = 500)
        String comment) {
}