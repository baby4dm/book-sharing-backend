package com.booksharing.enums;

/**
 * Відповідає Postgres-типу {@code dispute_status}.
 */
public enum DisputeStatus {
    OPEN,
    RESOLVED_FAVOR_FILER,
    RESOLVED_FAVOR_OTHER,
    DISMISSED
}