package com.booksharing.dto.res;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatRoomResponse(
        UUID id,
        UUID exchangeId,
        String exchangeStatus,
        String bookTitle,
        String bookCoverUrl,
        UUID otherUserId,
        String otherUserName,
        String otherUserAvatarUrl,
        String lastMessage,
        LocalDateTime lastMessageAt,
        UUID lastMessageSenderId,
        long unreadCount) {
}