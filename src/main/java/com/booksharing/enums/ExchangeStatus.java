package com.booksharing.enums;

/**
 * Відповідає Postgres-типу {@code exchange_status}.
 */
public enum ExchangeStatus {
    HANDOVER_PENDING,
    IN_READING,
    RETURN_PENDING,
    COMPLETED,
    OVERDUE,
    DISPUTED
}