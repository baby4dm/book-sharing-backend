package com.booksharing.model.notification;

/**
 * Типи сповіщень. На відміну від enum-ів в інших модулях, тут немає
 * нативного Postgres ENUM-типу — стовпець {@code notifications.type} це
 * звичайний {@code VARCHAR(50)}, тому мапиться стандартним
 * {@code @Enumerated(EnumType.STRING)} без {@code @JdbcType} (та обхідна
 * анотація потрібна лише для справжніх Postgres ENUM-типів).
 */
public enum NotificationType {
    NEW_REQUEST,
    REQUEST_APPROVED,
    REQUEST_REJECTED,
    DEADLINE_SOON,
    DEADLINE_OVERDUE,
    EXTENSION_REQUESTED,
    EXTENSION_DECIDED,
    DISPUTE_FILED,
    DISPUTE_RESOLVED,
    NEW_CHAT_MESSAGE,
    NEW_COMMENT,
    REVIEW_RECEIVED
}