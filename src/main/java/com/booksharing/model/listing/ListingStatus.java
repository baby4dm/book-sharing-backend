package com.booksharing.model.listing;

/**
 * Відповідає Postgres-типу {@code listing_status}.
 */
public enum ListingStatus {
    AVAILABLE,
    RESERVED,
    IN_EXCHANGE,
    ARCHIVED
}