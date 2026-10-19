package com.booksharing.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRequestRequest(
        @NotNull(message = "Вкажіть, скільки днів потрібно на читання")
        @Min(value = 1, message = "Мінімум 1 день на читання")
        @Max(value = 90, message = "Максимум 90 днів на читання")
        Integer readingDays,

        @NotBlank(message = "Потрібно обрати спосіб доставки")
        String preferredDeliveryMethod,

        @Size(max = 2000)
        String message) {
}