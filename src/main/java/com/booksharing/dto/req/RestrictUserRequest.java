package com.booksharing.dto.req;

import com.booksharing.enums.RestrictionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RestrictUserRequest(
        @NotNull(message = "Оберіть тип обмеження")
        RestrictionType type,

        @Min(value = 1, message = "Щонайменше 1 день")
        @Max(value = 365, message = "Не більше 365 днів")
        Integer days,

        @NotBlank(message = "Вкажіть причину")
        @Size(max = 1000, message = "Причина занадто довга")
        String reason) {
}
