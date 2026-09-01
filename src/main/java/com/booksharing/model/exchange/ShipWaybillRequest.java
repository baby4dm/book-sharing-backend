package com.booksharing.model.exchange;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShipWaybillRequest(
        @NotBlank(message = "Фото/скан накладної обов'язковий")
        @Size(max = 512)
        String waybillPhotoUrl) {
}