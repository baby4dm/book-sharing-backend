package com.booksharing.model.exchange;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddExchangePhotoRequest(
        @NotBlank(message = "URL фото обов'язковий")
        @Size(max = 512)
        String url,

        @Size(max = 2000)
        String note) {
}