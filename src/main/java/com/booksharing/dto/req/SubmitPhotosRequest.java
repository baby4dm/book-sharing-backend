package com.booksharing.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Пакет фото одного етапу обміну: від 1 до 4 URL (файли вже завантажені
 * через POST /api/uploads). Надсилається одним запитом, щоб етап
 * зберігався атомарно, а не по одному фото.
 */
public record SubmitPhotosRequest(
        @NotEmpty(message = "Додайте хоча б одне фото")
        @Size(max = 4, message = "Не більше 4 фото")
        List<@NotBlank @Size(max = 512) String> urls) {
}