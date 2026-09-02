package com.booksharing.repository;

import java.util.List;
import java.util.UUID;

import com.booksharing.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(UUID chatRoomId);
}
