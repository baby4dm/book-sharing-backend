package com.booksharing.dto.res;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatRoomResponse(
        UUID id,
        UUID exchangeId,
        UUID otherUserId,
        String otherUserName,
        String otherUserAvatarUrl,
        String lastMessage,
        LocalDateTime lastMessageAt,
        long unreadCount) {
}