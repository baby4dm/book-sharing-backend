package com.booksharing.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull(message = "Потрібно вказати оцінку")
        @Min(value = 1, message = "Мінімальна оцінка 1")
        @Max(value = 5, message = "Максимальна оцінка 5")
        Integer rating,

        @Size(max = 1000, message = "Коментар не довший за 1000 символів")
        String comment) {
}