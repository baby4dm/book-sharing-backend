package com.booksharing.enums;

public enum NotificationType {
    NEW_REQUEST,
    REQUEST_APPROVED,
    REQUEST_REJECTED,
    HANDOVER_READY,
    SHIPMENT_ADDRESS_ADDED,
    BOOK_SHIPPED,
    BOOK_RECEIVED,
    RETURN_STARTED,
    EXCHANGE_COMPLETED,
    DEADLINE_SOON,
    DEADLINE_OVERDUE,
    EXTENSION_REQUESTED,
    EXTENSION_DECIDED,
    DISPUTE_FILED,
    DISPUTE_RESOLVED,
    @Deprecated
    NEW_CHAT_MESSAGE,
    NEW_COMMENT,
    REVIEW_RECEIVED,
    FEEDBACK_RECEIVED,
    /** Модератор приховав оголошення користувача (referenceId — оголошення) */
    LISTING_HIDDEN,
    /** Модератор повернув оголошення в каталог (referenceId — оголошення) */
    LISTING_RESTORED,
    /** Модератор видалив пост або коментар користувача */
    CONTENT_REMOVED,
    /** Обліковий запис обмежено або заблоковано */
    ACCOUNT_RESTRICTED,
    /** Обмеження з облікового запису знято */
    ACCOUNT_RESTORED
}