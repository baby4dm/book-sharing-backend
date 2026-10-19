package com.booksharing.dto.res;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatEvent(
        Type type,
        UUID chatRoomId,
        ChatMessageResponse message,
        UUID userId,
        LocalDateTime readAt) {

    public enum Type { MESSAGE, READ, TYPING }

    public static ChatEvent ofMessage(ChatMessageResponse message) {
        return new ChatEvent(Type.MESSAGE, message.chatRoomId(), message, message.senderId(), null);
    }

    public static ChatEvent ofRead(UUID chatRoomId, UUID readerId, LocalDateTime readAt) {
        return new ChatEvent(Type.READ, chatRoomId, null, readerId, readAt);
    }

    public static ChatEvent ofTyping(UUID chatRoomId, UUID userId) {
        return new ChatEvent(Type.TYPING, chatRoomId, null, userId, null);
    }
}