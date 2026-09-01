package com.booksharing.model.bookcatalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Тіло POST /api/book-catalog/resolve. {@code source} тут навмисно немає:
 * сервіс сам визначає його — {@code externalId != null} означає, що
 * дані прийшли з пошуку Google Books, інакше це ручне заповнення
 * (fallback, коли API нічого не знайшло).
 */
public record ResolveBookCatalogEntryRequest(

        @NotBlank(message = "Назва книги обов'язкова")
        @Size(max = 500)
        String title,

        @Size(max = 20)
        String isbn,

        @Size(max = 500)
        String author,

        String description,

        @Size(max = 255)
        String genre,

        @Size(max = 512)
        String coverUrl,

        @Size(max = 100)
        String externalId) {
}