package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.SendMessageRequest;
import com.booksharing.dto.res.ChatEvent;
import com.booksharing.dto.res.ChatMessagePageResponse;
import com.booksharing.dto.res.ChatMessageResponse;
import com.booksharing.dto.res.ChatRoomResponse;
import com.booksharing.entity.ChatMessage;
import com.booksharing.entity.ChatRoom;
import com.booksharing.entity.Exchange;
import com.booksharing.entity.User;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.repository.ChatMessageRepository;
import com.booksharing.repository.ChatRoomRepository;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.UserRepository;
import com.booksharing.support.TestData;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Чати між користувачами")
class ChatServiceTest {

    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private ExchangeRepository exchangeRepository;
    @Mock private UserRepository userRepository;
    @Mock private ChatEventPublisher eventPublisher;

    @InjectMocks
    private ChatService chatService;

    private User me;
    private User other;
    private ChatRoom room;

    @BeforeEach
    void setUp() {
        me = TestData.user("Я");
        other = TestData.user("Співрозмовник");
        room = ChatRoom.builder().id(UUID.randomUUID()).userA(me).userB(other).build();
        when(chatRoomRepository.findWithDetailsById(room.getId())).thenReturn(Optional.of(room));
        when(chatMessageRepository.save(any(ChatMessage.class))).thenAnswer(inv -> {
            ChatMessage m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            m.setCreatedAt(LocalDateTime.now());
            return m;
        });
    }

    private ChatMessage message(User sender, String text, LocalDateTime at) {
        return ChatMessage.builder().id(UUID.randomUUID()).chatRoom(room).sender(sender).content(text).createdAt(at).build();
    }

    private static List<Object[]> rows(Object[]... rows) {
        return new ArrayList<>(List.of(rows));
    }

    @Test
    @DisplayName("список чатів: останнє повідомлення, непрочитані й актуальний обмін")
    void myRooms() {
        User third = TestData.user("Третій");
        ChatRoom quiet = ChatRoom.builder().id(UUID.randomUUID()).userA(third).userB(me).build();
        when(chatRoomRepository.findAllForUser(me.getId())).thenReturn(List.of(quiet, room));
        ChatMessage last = message(other, "Привіт", LocalDateTime.of(2026, 10, 1, 9, 0));
        when(chatMessageRepository.findLastMessages(anyCollection())).thenReturn(List.of(last));
        when(chatMessageRepository.countUnreadByRoom(anyCollection(), eq(me.getId())))
                .thenReturn(rows(new Object[]{room.getId(), 3L}));
        Exchange done = TestData.exchange(me, other, DeliveryMethod.PICKUP, ExchangeStatus.COMPLETED);
        Exchange active = TestData.exchange(other, me, DeliveryMethod.MAIL, ExchangeStatus.IN_READING);
        when(chatRoomRepository.findExchangesOfUser(me.getId())).thenReturn(List.of(done, active));

        List<ChatRoomResponse> result = chatService.getMyChatRooms(me.getId());

        assertThat(result).hasSize(2);
        ChatRoomResponse first = result.get(0);
        assertThat(first.id()).isEqualTo(room.getId());
        assertThat(first.lastMessage()).isEqualTo("Привіт");
        assertThat(first.lastMessageSenderId()).isEqualTo(other.getId());
        assertThat(first.unreadCount()).isEqualTo(3);
        assertThat(first.exchangeId()).isEqualTo(active.getId());
        assertThat(first.exchangeStatus()).isEqualTo("IN_READING");
        assertThat(first.bookTitle()).isEqualTo("Лісова пісня");
        assertThat(first.otherUserName()).isEqualTo("Співрозмовник");
        ChatRoomResponse second = result.get(1);
        assertThat(second.otherUserId()).isEqualTo(third.getId());
        assertThat(second.lastMessage()).isNull();
        assertThat(second.exchangeId()).isNull();
        assertThat(second.unreadCount()).isZero();
    }

    @Test
    @DisplayName("без чатів повертається порожній список")
    void noRooms() {
        when(chatRoomRepository.findAllForUser(me.getId())).thenReturn(List.of());
        assertThat(chatService.getMyChatRooms(me.getId())).isEmpty();
        verify(chatMessageRepository, never()).findLastMessages(any());
    }

    @Test
    @DisplayName("якщо всі обміни завершено, показується перший з них")
    void onlyCompletedExchanges() {
        Exchange done = TestData.exchange(other, me, DeliveryMethod.PICKUP, ExchangeStatus.COMPLETED);
        when(chatRoomRepository.findExchangesOfUser(other.getId())).thenReturn(List.of(done));

        ChatRoomResponse response = chatService.getRoom(room.getId(), other.getId());

        assertThat(response.otherUserId()).isEqualTo(me.getId());
        assertThat(response.exchangeId()).isEqualTo(done.getId());
    }

    @Test
    @DisplayName("окремий чат: непрочитані рахуються з першого рядка")
    void singleRoom() {
        when(chatMessageRepository.countUnreadByRoom(anyCollection(), eq(me.getId())))
                .thenReturn(rows(new Object[]{room.getId(), 5L}));
        ChatRoomResponse response = chatService.getRoom(room.getId(), me.getId());
        assertThat(response.unreadCount()).isEqualTo(5);
        assertThat(response.exchangeId()).isNull();
    }

    @Test
    @DisplayName("сторонній не бачить чужий чат")
    void strangerCannotRead() {
        assertThatThrownBy(() -> chatService.getRoom(room.getId(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("неіснуючий чат")
    void roomNotFound() {
        assertThatThrownBy(() -> chatService.getRoom(UUID.randomUUID(), me.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("чат обміну створюється, якщо його ще немає")
    void roomByExchange() {
        Exchange exchange = TestData.exchange(me, other, DeliveryMethod.PICKUP, ExchangeStatus.IN_READING);
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        when(chatRoomRepository.findBetween(me.getId(), other.getId())).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(room);

        ChatRoomResponse response = chatService.getRoomByExchange(exchange.getId(), other.getId());

        assertThat(response.id()).isEqualTo(room.getId());
        verify(chatRoomRepository).save(any(ChatRoom.class));
    }

    @Test
    @DisplayName("чат обміну доступний лише учасникам")
    void roomByExchangeStranger() {
        Exchange exchange = TestData.exchange(me, other, DeliveryMethod.PICKUP, ExchangeStatus.IN_READING);
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        assertThatThrownBy(() -> chatService.getRoomByExchange(exchange.getId(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> chatService.getRoomByExchange(UUID.randomUUID(), me.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("чат з користувачем: наявний чат перевикористовується")
    void roomWithUser() {
        when(userRepository.findById(other.getId())).thenReturn(Optional.of(other));
        when(chatRoomRepository.findBetween(me.getId(), other.getId())).thenReturn(Optional.of(room));

        ChatRoomResponse response = chatService.getRoomWithUser(other.getId(), me);

        assertThat(response.id()).isEqualTo(room.getId());
        verify(chatRoomRepository, never()).save(any());
    }

    @Test
    @DisplayName("не можна написати самому собі чи неіснуючому користувачу")
    void roomWithUserErrors() {
        assertThatThrownBy(() -> chatService.getRoomWithUser(me.getId(), me))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chatService.getRoomWithUser(UUID.randomUUID(), me))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("кількість непрочитаних")
    void unread() {
        when(chatMessageRepository.countUnreadForUser(me.getId())).thenReturn(7L);
        assertThat(chatService.getUnreadCount(me.getId()).count()).isEqualTo(7);
    }

    @Test
    @DisplayName("сторінка повідомлень у хронологічному порядку з ознакою продовження")
    void messagesPage() {
        LocalDateTime t = LocalDateTime.of(2026, 10, 1, 12, 0);
        List<ChatMessage> newestFirst = List.of(
                message(me, "3", t.plusMinutes(3)), message(other, "2", t.plusMinutes(2)), message(me, "1", t.plusMinutes(1)));
        when(chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(eq(room.getId()), any(Pageable.class)))
                .thenReturn(newestFirst);

        ChatMessagePageResponse page = chatService.getMessages(room.getId(), me.getId(), null, 2);

        assertThat(page.hasMore()).isTrue();
        assertThat(page.items()).extracting(ChatMessageResponse::content).containsExactly("2", "3");
    }

    @Test
    @DisplayName("старіші повідомлення підвантажуються від заданої дати")
    void messagesBefore() {
        LocalDateTime before = LocalDateTime.of(2026, 10, 1, 12, 0);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(chatMessageRepository.findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                eq(room.getId()), eq(before), pageable.capture()))
                .thenReturn(List.of(message(other, "старе", before.minusDays(1))));

        ChatMessagePageResponse page = chatService.getMessages(room.getId(), me.getId(), before, null);

        assertThat(page.hasMore()).isFalse();
        assertThat(page.items()).hasSize(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(31);
    }

    @Test
    @DisplayName("розмір сторінки обмежується сотнею")
    void messagesPageLimit() {
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        when(chatMessageRepository.findByChatRoomIdOrderByCreatedAtDesc(eq(room.getId()), pageable.capture()))
                .thenReturn(List.of());
        chatService.getMessages(room.getId(), me.getId(), null, 1000);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(101);
    }

    @Test
    @DisplayName("надсилання повідомлення розсилає подію обом учасникам")
    void send() {
        ChatMessageResponse response = chatService.sendMessage(room.getId(), me, new SendMessageRequest("  Привіт  "));

        assertThat(response.content()).isEqualTo("Привіт");
        assertThat(response.senderId()).isEqualTo(me.getId());
        ArgumentCaptor<ChatEvent> events = ArgumentCaptor.forClass(ChatEvent.class);
        verify(eventPublisher).sendTo(eq(other.getId()), events.capture());
        assertThat(events.getValue().type()).isEqualTo(ChatEvent.Type.MESSAGE);
        verify(eventPublisher).sendTo(eq(me.getId()), any(ChatEvent.class));
    }

    @Test
    @DisplayName("відповідь позначає вхідні повідомлення прочитаними")
    void sendMarksIncomingRead() {
        when(chatMessageRepository.markIncomingAsRead(eq(room.getId()), eq(me.getId()), any())).thenReturn(2);
        chatService.sendMessage(room.getId(), me, new SendMessageRequest("Ок"));
        ArgumentCaptor<ChatEvent> events = ArgumentCaptor.forClass(ChatEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).sendTo(eq(other.getId()), events.capture());
        assertThat(events.getAllValues()).extracting(ChatEvent::type)
                .containsExactly(ChatEvent.Type.READ, ChatEvent.Type.MESSAGE);
    }

    @Test
    @DisplayName("порожнє повідомлення не надсилається")
    void sendEmpty() {
        assertThatThrownBy(() -> chatService.sendMessage(room.getId(), me, new SendMessageRequest("   ")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> chatService.sendMessage(room.getId(), me, new SendMessageRequest(null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(chatMessageRepository, never()).save(any());
    }

    @Test
    @DisplayName("позначення прочитаним сповіщає співрозмовника лише якщо щось змінилося")
    void markAsRead() {
        when(chatMessageRepository.markIncomingAsRead(eq(room.getId()), eq(other.getId()), any())).thenReturn(0, 4);

        chatService.markAsRead(room.getId(), other.getId());
        verify(eventPublisher, never()).sendTo(any(), any());

        chatService.markAsRead(room.getId(), other.getId());
        verify(eventPublisher).sendTo(eq(me.getId()), any(ChatEvent.class));
        verify(eventPublisher).sendTo(eq(other.getId()), any(ChatEvent.class));
    }

    @Test
    @DisplayName("подія набору тексту йде лише співрозмовнику")
    void typing() {
        chatService.notifyTyping(room.getId(), me.getId());
        ArgumentCaptor<ChatEvent> event = ArgumentCaptor.forClass(ChatEvent.class);
        verify(eventPublisher).sendTo(eq(other.getId()), event.capture());
        assertThat(event.getValue().type()).isEqualTo(ChatEvent.Type.TYPING);
        assertThat(event.getValue().userId()).isEqualTo(me.getId());
    }
}
