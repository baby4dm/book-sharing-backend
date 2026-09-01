package com.booksharing.model.chat;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.model.user.User;
import com.booksharing.model.user.UserRepository;
import java.security.Principal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * {@code Principal.getName()} тут - це userId (рядком), який поклав
 * {@link com.booksharing.security.StompAuthChannelInterceptor} під час
 * STOMP CONNECT, а не email чи щось інше.
 */
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{chatRoomId}/send")
    public void send(@DestinationVariable UUID chatRoomId, SendMessageRequest request, Principal principal) {
        UUID senderId = UUID.fromString(principal.getName());
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено: " + senderId));

        ChatMessageResponse response = chatService.sendMessage(chatRoomId, sender, request);

        messagingTemplate.convertAndSend("/topic/chat/" + chatRoomId, response);
    }
}