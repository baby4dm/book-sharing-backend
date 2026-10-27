package com.booksharing.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerationReasonRequest(
        @NotBlank(message = "Вкажіть причину")
        @Size(max = 1000, message = "Причина занадто довга")
        String reason) {
}
