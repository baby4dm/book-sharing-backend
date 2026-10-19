package com.booksharing.controller;

import com.booksharing.dto.req.SendMessageRequest;
import com.booksharing.dto.res.ChatMessagePageResponse;
import com.booksharing.dto.res.ChatMessageResponse;
import com.booksharing.dto.res.ChatRoomResponse;
import com.booksharing.dto.res.UnreadCountResponse;
import com.booksharing.entity.User;
import com.booksharing.service.ChatService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @GetMapping
    public List<ChatRoomResponse> getMyChatRooms(@AuthenticationPrincipal User currentUser) {
        return chatService.getMyChatRooms(currentUser.getId());
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(@AuthenticationPrincipal User currentUser) {
        return chatService.getUnreadCount(currentUser.getId());
    }

    @GetMapping("/by-exchange/{exchangeId}")
    public ChatRoomResponse getByExchange(
            @PathVariable UUID exchangeId, @AuthenticationPrincipal User currentUser) {
        return chatService.getRoomByExchange(exchangeId, currentUser.getId());
    }

    @GetMapping("/with/{userId}")
    public ChatRoomResponse getWithUser(
            @PathVariable UUID userId, @AuthenticationPrincipal User currentUser) {
        return chatService.getRoomWithUser(userId, currentUser);
    }

    @GetMapping("/{id}")
    public ChatRoomResponse getRoom(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return chatService.getRoom(id, currentUser.getId());
    }

    @GetMapping("/{id}/messages")
    public ChatMessagePageResponse getMessages(
            @PathVariable UUID id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
            @RequestParam(required = false) Integer size,
            @AuthenticationPrincipal User currentUser) {
        return chatService.getMessages(id, currentUser.getId(), before, size);
    }

    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SendMessageRequest request) {
        return chatService.sendMessage(id, currentUser, request);
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void markAsRead(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        chatService.markAsRead(id, currentUser.getId());
    }
}