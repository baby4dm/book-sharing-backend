package com.booksharing.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OpenDisputeRequest(
        @NotBlank(message = "Вкажіть причину спору")
        @Size(max = 2000)
        String reason) {
}