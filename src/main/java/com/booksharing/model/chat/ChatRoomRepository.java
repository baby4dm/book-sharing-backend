package com.booksharing.model.chat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    Optional<ChatRoom> findByExchangeId(UUID exchangeId);

    List<ChatRoom> findByUserAIdOrUserBId(UUID userAId, UUID userBId);
}
