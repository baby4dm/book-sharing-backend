package com.booksharing.dto.req;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * {@code preferredDeliveryMethod} приймається як рядок (не enum
 * напряму) і звіряється в сервісі з переліком способів, які підтримує
 * конкретний {@code Listing} - той самий підхід, що й у
 * {@code CreateListingRequest.deliveryMethods}.
 */
public record CreateRequestRequest(

        @NotNull(message = "Потрібно вказати бажаний дедлайн повернення")
        @Future(message = "Дедлайн має бути в майбутньому")
        LocalDate desiredDeadline,

        @NotBlank(message = "Потрібно обрати спосіб доставки")
        String preferredDeliveryMethod,

        @Size(max = 2000)
        String message) {
}