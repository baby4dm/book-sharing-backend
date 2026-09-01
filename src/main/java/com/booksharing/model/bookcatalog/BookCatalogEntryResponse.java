package com.booksharing.model.bookcatalog;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookCatalogEntryResponse(
        UUID id,
        String isbn,
        String title,
        String author,
        String description,
        String genre,
        String coverUrl,
        BookSource source,
        String externalId,
        LocalDateTime createdAt) {
}