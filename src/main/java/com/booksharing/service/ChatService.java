package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.SendMessageRequest;
import com.booksharing.dto.res.ChatEvent;
import com.booksharing.dto.res.ChatMessagePageResponse;
import com.booksharing.dto.res.ChatMessageResponse;
import com.booksharing.dto.res.ChatRoomResponse;
import com.booksharing.dto.res.UnreadCountResponse;
import com.booksharing.entity.BookCatalogEntry;
import com.booksharing.entity.ChatMessage;
import com.booksharing.entity.ChatRoom;
import com.booksharing.entity.Exchange;
import com.booksharing.entity.User;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.repository.ChatMessageRepository;
import com.booksharing.repository.ChatRoomRepository;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ExchangeRepository exchangeRepository;
    private final UserRepository userRepository;
    private final ChatEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getMyChatRooms(UUID currentUserId) {
        List<ChatRoom> rooms = chatRoomRepository.findAllForUser(currentUserId);
        if (rooms.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = rooms.stream().map(ChatRoom::getId).toList();

        Map<UUID, ChatMessage> lastByRoom = chatMessageRepository.findLastMessages(ids).stream()
                .collect(Collectors.toMap(m -> m.getChatRoom().getId(), Function.identity(),
                        (a, b) -> a));
        Map<UUID, Long> unreadByRoom = chatMessageRepository.countUnreadByRoom(ids, currentUserId).stream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
        Map<UUID, List<Exchange>> exchangesByPartner = chatRoomRepository.findExchangesOfUser(currentUserId)
                .stream()
                .collect(Collectors.groupingBy(e -> partnerId(e, currentUserId)));

        return rooms.stream()
                .map(room -> toRoomResponse(room, currentUserId,
                        lastByRoom.get(room.getId()),
                        unreadByRoom.getOrDefault(room.getId(), 0L),
                        relevantExchange(exchangesByPartner.get(otherParticipant(room, currentUserId).getId()))))
                .sorted(Comparator.comparing(
                                (ChatRoomResponse r) -> r.lastMessageAt() != null ? r.lastMessageAt() : LocalDateTime.MIN)
                        .reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getRoom(UUID chatRoomId, UUID currentUserId) {
        ChatRoom room = requireParticipant(chatRoomId, currentUserId);
        List<UUID> ids = List.of(room.getId());
        ChatMessage last = chatMessageRepository.findLastMessages(ids).stream().findFirst().orElse(null);
        long unread = chatMessageRepository.countUnreadByRoom(ids, currentUserId).stream()
                .findFirst().map(row -> (Long) row[1]).orElse(0L);
        UUID otherId = otherParticipant(room, currentUserId).getId();
        List<Exchange> withOther = chatRoomRepository.findExchangesOfUser(currentUserId).stream()
                .filter(e -> partnerId(e, currentUserId).equals(otherId))
                .toList();
        return toRoomResponse(room, currentUserId, last, unread, relevantExchange(withOther));
    }

    @Transactional
    public ChatRoomResponse getRoomByExchange(UUID exchangeId, UUID currentUserId) {
        Exchange exchange = exchangeRepository.findById(exchangeId)
                .orElseThrow(() -> new ResourceNotFoundException("Обмін не знайдено: " + exchangeId));
        boolean isOwner = exchange.getOwner().getId().equals(currentUserId);
        boolean isReader = exchange.getReader().getId().equals(currentUserId);
        if (!isOwner && !isReader) {
            throw new AccessDeniedException("Ви не берете участі в цьому обміні");
        }
        ChatRoom room = getOrCreateRoom(exchange.getOwner(), exchange.getReader());
        return getRoom(room.getId(), currentUserId);
    }

    @Transactional
    public ChatRoomResponse getRoomWithUser(UUID otherUserId, User currentUser) {
        if (otherUserId.equals(currentUser.getId())) {
            throw new IllegalArgumentException("Не можна написати самому собі");
        }
        User other = userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено: " + otherUserId));
        ChatRoom room = getOrCreateRoom(currentUser, other);
        return getRoom(room.getId(), currentUser.getId());
    }

    @Transactional
    public ChatRoom getOrCreateRoom(User first, User second) {
        return chatRoomRepository.findBetween(first.getId(), second.getId())
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .userA(first)
                        .userB(second)
                        .build()));
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(UUID currentUserId) {
        return new UnreadCountResponse(chatMessageRepository.countUnreadForUser(currentUserId));
    }

    @Transactional(readOnly = true)
    public ChatMessagePageResponse getMessages(
            UUID chatRoomId, UUID currentUserId, LocalDateTime before, Integer size) {
        requireParticipant(chatRoomId, currentUserId);
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest page = PageRequest.of(0, pageSize + 1);

        List<ChatMessage> newestFirst = before == null
                ? chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(chatRoomId, page)
                : chatMessageRepository.findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                chatRoomId, before, page);

        boolean hasMore = newestFirst.size() > pageSize;
        List<ChatMessage> slice = new ArrayList<>(
                hasMore ? newestFirst.subList(0, pageSize) : newestFirst);
        Collections.reverse(slice);

        return new ChatMessagePageResponse(slice.stream().map(this::toMessageResponse).toList(), hasMore);
    }

    @Transactional
    public ChatMessageResponse sendMessage(UUID chatRoomId, User sender, SendMessageRequest request) {
        ChatRoom room = requireParticipant(chatRoomId, sender.getId());
        String content = request.content() == null ? "" : request.content().trim();
        if (content.isEmpty()) {
            throw new IllegalArgumentException("Повідомлення не може бути порожнім");
        }

        ChatMessage message = chatMessageRepository.save(ChatMessage.builder()
                .chatRoom(room)
                .sender(sender)
                .content(content)
                .build());

        User recipient = otherParticipant(room, sender.getId());

        LocalDateTime now = LocalDateTime.now();
        if (chatMessageRepository.markIncomingAsRead(room.getId(), sender.getId(), now) > 0) {
            ChatEvent read = ChatEvent.ofRead(room.getId(), sender.getId(), now);
            eventPublisher.sendTo(recipient.getId(), read);
            eventPublisher.sendTo(sender.getId(), read);
        }

        ChatMessageResponse response = toMessageResponse(message);
        ChatEvent event = ChatEvent.ofMessage(response);
        eventPublisher.sendTo(recipient.getId(), event);
        eventPublisher.sendTo(sender.getId(), event);
        return response;
    }

    @Transactional
    public void markAsRead(UUID chatRoomId, UUID currentUserId) {
        ChatRoom room = requireParticipant(chatRoomId, currentUserId);
        LocalDateTime now = LocalDateTime.now();
        int updated = chatMessageRepository.markIncomingAsRead(chatRoomId, currentUserId, now);
        if (updated > 0) {
            ChatEvent event = ChatEvent.ofRead(chatRoomId, currentUserId, now);
            eventPublisher.sendTo(otherParticipant(room, currentUserId).getId(), event);
            // власним вкладкам — щоб зник бейдж непрочитаних
            eventPublisher.sendTo(currentUserId, event);
        }
    }

    @Transactional(readOnly = true)
    public void notifyTyping(UUID chatRoomId, UUID currentUserId) {
        ChatRoom room = requireParticipant(chatRoomId, currentUserId);
        eventPublisher.sendTo(otherParticipant(room, currentUserId).getId(),
                ChatEvent.ofTyping(chatRoomId, currentUserId));
    }

    private ChatRoom requireParticipant(UUID chatRoomId, UUID currentUserId) {
        ChatRoom room = chatRoomRepository.findWithDetailsById(chatRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Чат не знайдено: " + chatRoomId));
        boolean isParticipant = room.getUserA().getId().equals(currentUserId)
                || room.getUserB().getId().equals(currentUserId);
        if (!isParticipant) {
            throw new AccessDeniedException("Ви не берете участі в цьому чаті");
        }
        return room;
    }

    private static User otherParticipant(ChatRoom room, UUID userId) {
        return room.getUserA().getId().equals(userId) ? room.getUserB() : room.getUserA();
    }

    private static UUID partnerId(Exchange exchange, UUID currentUserId) {
        return exchange.getOwner().getId().equals(currentUserId)
                ? exchange.getReader().getId()
                : exchange.getOwner().getId();
    }

    private static Exchange relevantExchange(List<Exchange> exchanges) {
        if (exchanges == null || exchanges.isEmpty()) {
            return null;
        }
        return exchanges.stream()
                .filter(e -> e.getStatus() != ExchangeStatus.COMPLETED)
                .findFirst()
                .orElse(exchanges.get(0));
    }

    private ChatRoomResponse toRoomResponse(
            ChatRoom room, UUID currentUserId, ChatMessage last, long unread, Exchange exchange) {
        User other = otherParticipant(room, currentUserId);
        BookCatalogEntry book = exchange != null && exchange.getListing() != null
                ? exchange.getListing().getBookCatalogEntry()
                : null;

        return new ChatRoomResponse(
                room.getId(),
                exchange != null ? exchange.getId() : null,
                exchange != null && exchange.getStatus() != null ? exchange.getStatus().name() : null,
                book != null ? book.getTitle() : null,
                book != null ? book.getCoverUrl() : null,
                other.getId(),
                other.getName(),
                other.getAvatarUrl(),
                last != null ? last.getContent() : null,
                last != null ? last.getCreatedAt() : null,
                last != null ? last.getSender().getId() : null,
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