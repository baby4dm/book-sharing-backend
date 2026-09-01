package com.booksharing.model.chat;

import com.booksharing.model.user.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @GetMapping("/{id}/messages")
    public List<ChatMessageResponse> getMessages(
            @PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return chatService.getMessages(id, currentUser.getId());
    }

    /**
     * REST-фолбек для надсилання повідомлень без WebSocket (наприклад,
     * якщо з'єднання STOMP не встановилось). Основний шлях у реальному
     * часі - {@code /app/chat/{id}/send} через WebSocket, див. {@link ChatWebSocketController}.
     */
    @PostMapping("/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public ChatMessageResponse sendMessage(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody SendMessageRequest request) {
        return chatService.sendMessage(id, currentUser, request);
    }
}