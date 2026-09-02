package com.booksharing.dto.res;

import java.time.LocalDateTime;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID chatRoomId,
        UUID senderId,
        String senderName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime readAt) {
}