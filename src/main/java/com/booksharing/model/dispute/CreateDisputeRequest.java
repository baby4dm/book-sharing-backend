package com.booksharing.model.dispute;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDisputeRequest(
        @NotBlank(message = "Потрібно описати суть скарги")
        @Size(max = 4000)
        String description) {
}