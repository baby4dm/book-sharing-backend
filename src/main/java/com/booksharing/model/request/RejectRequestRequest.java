package com.booksharing.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectRequestRequest(
        @NotBlank(message = "Потрібно вказати причину відхилення")
        @Size(max = 2000)
        String comment) {
}