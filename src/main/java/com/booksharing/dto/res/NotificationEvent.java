package com.booksharing.dto.res;

import java.util.UUID;

public record NotificationEvent(
        Kind kind,
        NotificationResponse notification,
        UUID notificationId,
        long unreadCount) {

    public enum Kind { CREATED, READ, READ_ALL }

    public static NotificationEvent created(NotificationResponse notification, long unreadCount) {
        return new NotificationEvent(Kind.CREATED, notification, null, unreadCount);
    }

    public static NotificationEvent read(UUID notificationId, long unreadCount) {
        return new NotificationEvent(Kind.READ, null, notificationId, unreadCount);
    }

    public static NotificationEvent readAll() {
        return new NotificationEvent(Kind.READ_ALL, null, null, 0);
    }
}