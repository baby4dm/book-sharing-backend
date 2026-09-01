package com.booksharing.model.request;

/**
 * Відповідає Postgres-типу {@code request_status}.
 */
public enum RequestStatus {
    PENDING,
    ACTIVE,
    REJECTED,
    CANCELLED
}