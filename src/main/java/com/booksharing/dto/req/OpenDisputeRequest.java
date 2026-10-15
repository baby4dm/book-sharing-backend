package com.booksharing.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OpenDisputeRequest(
        @NotBlank(message = "Вкажіть причину спору")
        @Size(max = 2000)
        String reason,

        @NotEmpty(message = "Додайте хоча б одне фото-доказ")
        @Size(max = 4, message = "Не більше 4 фото")
        List<@NotBlank @Size(max = 512) String> urls) {
}