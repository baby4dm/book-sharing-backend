package com.booksharing.dto.req;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Тіло POST /api/listings. {@code bookCatalogEntryId} має бути вже
 * "розв'язаний" через POST /api/book-catalog/resolve перед створенням
 * оголошення (двофазний флоу з модуля BookCatalog). {@code deliveryMethods}
 * приймається як рядки (не enum), бо стовпець у БД - {@code text[]}, а не
 * нативний Postgres enum-масив (див. коментар у {@code Listing.java}).
 */
public record CreateListingRequest(

        @NotNull(message = "Потрібно вказати книгу з каталогу")
        UUID bookCatalogEntryId,

        @Size(max = 2000)
        String conditionDescription,

        @NotEmpty(message = "Потрібно вказати щонайменше один спосіб доставки")
        List<String> deliveryMethods,

        List<String> photoUrls) {
}