package com.booksharing.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddListingPhotoRequest(
        @NotBlank(message = "URL фото обов'язковий")
        @Size(max = 512)
        String url) {
}