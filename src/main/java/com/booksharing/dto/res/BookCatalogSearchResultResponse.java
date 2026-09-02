package com.booksharing.dto.res;

import com.booksharing.entity.BookCatalogEntry;

/**
 * Результат пошуку GET /api/book-catalog/search — ще не збережений у БД.
 * Стає {@link BookCatalogEntry} лише коли користувач обере конкретний
 * варіант і фронтенд викличе POST /api/book-catalog/resolve.
 */
public record BookCatalogSearchResultResponse(
        String title,
        String author,
        String description,
        String genre,
        String coverUrl,
        String isbn,
        String externalId) {
}