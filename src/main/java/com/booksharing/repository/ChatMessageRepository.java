package com.booksharing.repository;

import com.booksharing.entity.ChatMessage;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(UUID chatRoomId);

    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtDesc(UUID chatRoomId, Pageable pageable);

    @EntityGraph(attributePaths = "sender")
    List<ChatMessage> findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID chatRoomId, LocalDateTime before, Pageable pageable);

    @Query("""
            select m from ChatMessage m join fetch m.sender
            where m.chatRoom.id in :roomIds
              and m.createdAt = (select max(m2.createdAt) from ChatMessage m2
                                 where m2.chatRoom.id = m.chatRoom.id)
            """)
    List<ChatMessage> findLastMessages(@Param("roomIds") Collection<UUID> roomIds);

    @Query("""
            select m.chatRoom.id, count(m) from ChatMessage m
            where m.chatRoom.id in :roomIds
              and m.readAt is null
              and m.sender.id <> :userId
            group by m.chatRoom.id
            """)
    List<Object[]> countUnreadByRoom(
            @Param("roomIds") Collection<UUID> roomIds, @Param("userId") UUID userId);

    @Query("""
            select count(m) from ChatMessage m
            where m.readAt is null
              and m.sender.id <> :userId
              and (m.chatRoom.userA.id = :userId or m.chatRoom.userB.id = :userId)
            """)
    long countUnreadForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("""
            update ChatMessage m set m.readAt = :readAt
            where m.chatRoom.id = :roomId
              and m.readAt is null
              and m.sender.id <> :readerId
            """)
    int markIncomingAsRead(
            @Param("roomId") UUID roomId,
            @Param("readerId") UUID readerId,
            @Param("readAt") LocalDateTime readAt);
}