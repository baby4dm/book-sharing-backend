package com.booksharing.enums;

/**
 * Відповідає Postgres-типу {@code request_status}.
 */
public enum RequestStatus {
    PENDING,
    ACTIVE,
    REJECTED,
    CANCELLED
}