package com.booksharing.dto.req;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExtensionRequest(
        @NotNull(message = "Вкажіть, на скільки днів продовжити")
        @Min(value = 1, message = "Мінімум 1 день")
        @Max(value = 60, message = "Максимум 60 днів за один запит")
        Integer extraDays,

        @Size(max = 1000)
        String comment) {
}