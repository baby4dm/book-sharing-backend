package com.booksharing.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "Повідомлення не може бути порожнім")
        @Size(max = 4000)
        String content) {
}