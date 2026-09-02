package com.booksharing.dto.req;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateExtensionRequest(
        @NotNull(message = "Потрібно вказати новий бажаний дедлайн")
        @Future(message = "Новий дедлайн має бути в майбутньому")
        LocalDate requestedNewDeadline,

        @Size(max = 1000)
        String comment) {
}