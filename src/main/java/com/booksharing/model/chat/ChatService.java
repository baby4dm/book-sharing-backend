package com.booksharing.model.chat;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.model.user.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;

    public List<ChatRoomResponse> getMyChatRooms(UUID currentUserId) {
        return chatRoomRepository.findByUserAIdOrUserBId(currentUserId, currentUserId).stream()
                .map(room -> toRoomResponse(room, currentUserId))
                .toList();
    }

    /** Відкриття чату одразу позначає вхідні непрочитані повідомлення прочитаними. */
    @Transactional
    public List<ChatMessageResponse> getMessages(UUID chatRoomId, UUID currentUserId) {
        ChatRoom room = requireParticipant(chatRoomId, currentUserId);

        List<ChatMessage> messages = chatMessageRepository
                .findByChatRoomIdOrderByCreatedAtAsc(chatRoomId);

        messages.stream()
                .filter(m -> m.getReadAt() == null && !m.getSender().getId().equals(currentUserId))
                .forEach(m -> {
                    m.setReadAt(LocalDateTime.now());
                    chatMessageRepository.save(m);
                });

        return messages.stream().map(this::toMessageResponse).toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID chatRoomId, User sender, SendMessageRequest request) {
        ChatRoom room = requireParticipant(chatRoomId, sender.getId());

        ChatMessage message = ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(request.content())
                .build();
        message = chatMessageRepository.save(message);

        return toMessageResponse(message);
    }

    private ChatRoom requireParticipant(UUID chatRoomId, UUID currentUserId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат не знайдено: " + chatRoomId));
        boolean isParticipant = room.getUserA().getId().equals(currentUserId)
                || room.getUserB().getId().equals(currentUserId);
        if (!isParticipant) {
            throw new AccessDeniedException("Ви не берете участі в цьому чаті");
        }
        return room;
    }

    private ChatRoomResponse toRoomResponse(ChatRoom room, UUID currentUserId) {
        User other = room.getUserA().getId().equals(currentUserId) ? room.getUserB() : room.getUserA();

        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(room.getId());
        ChatMessage last = messages.isEmpty() ? null : messages.get(messages.size() - 1);
        long unread = messages.stream()
                .filter(m -> m.getReadAt() == null && !m.getSender().getId().equals(currentUserId))
                .count();

        return new ChatRoomResponse(
                room.getId(),
                room.getExchange() != null ? room.getExchange().getId() : null,
                other.getId(),
                other.getName(),
                other.getAvatarUrl(),
                last != null ? last.getContent() : null,
                last != null ? last.getCreatedAt() : null,
                unread);
    }

    private ChatMessageResponse toMessageResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSender().getId(),
                message.getSender().getName(),
                message.getContent(),
                message.getCreatedAt(),
                message.getReadAt());
    }
}