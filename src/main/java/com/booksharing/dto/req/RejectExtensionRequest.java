package com.booksharing.dto.req;

import jakarta.validation.constraints.Size;

public record RejectExtensionRequest(
        @Size(max = 1000)
        String comment) {
}