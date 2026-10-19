package com.booksharing.controller;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.SendMessageRequest;
import com.booksharing.entity.User;
import com.booksharing.repository.UserRepository;
import com.booksharing.service.ChatService;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;


@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @MessageMapping("/chat/{chatRoomId}/send")
    public void send(@DestinationVariable UUID chatRoomId, SendMessageRequest request, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено: " + senderId));
        chatService.sendMessage(chatRoomId, sender, request);
    }

    @MessageMapping("/chat/{chatRoomId}/typing")
    public void typing(@DestinationVariable UUID chatRoomId, Principal principal) {
        chatService.notifyTyping(chatRoomId, UUID.fromString(principal.getName()));
    }
}