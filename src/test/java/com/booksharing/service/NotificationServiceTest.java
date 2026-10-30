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
import com.booksharing.dto.res.NotificationEvent;
import com.booksharing.dto.res.NotificationPageResponse;
import com.booksharing.entity.Notification;
import com.booksharing.entity.User;
import com.booksharing.enums.NotificationType;
import com.booksharing.repository.NotificationRepository;
import com.booksharing.support.TestData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
@DisplayName("Сповіщення")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationLinkResolver linkResolver;
    @Mock private NotificationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = TestData.user("Користувач");
        when(linkResolver.resolve(anyCollection())).thenReturn(Map.of());
    }

    private Notification notification(boolean read) {
        return Notification.builder().id(UUID.randomUUID()).user(user).type(NotificationType.NEW_REQUEST)
                .message("Нова заявка").isRead(read).createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("сторінка сповіщень з посиланнями й ознакою продовження")
    void page() {
        Notification a = notification(false);
        Notification b = notification(true);
        Notification c = notification(true);
        when(notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(eq(user.getId()), any(Pageable.class)))
                .thenReturn(List.of(a, b, c));
        when(linkResolver.resolve(anyCollection())).thenReturn(Map.of(a.getId(), "/requests"));

        NotificationPageResponse page = notificationService.getMyNotifications(user.getId(), null, false, 2);

        assertThat(page.hasMore()).isTrue();
        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0).link()).isEqualTo("/requests");
        assertThat(page.items().get(0).read()).isFalse();
        assertThat(page.items().get(1).read()).isTrue();
    }

    @Test
    @DisplayName("кожен варіант вибірки: усі чи непрочитані, з датою чи без")
    void queryVariants() {
        LocalDateTime before = LocalDateTime.now();
        when(notificationRepository.findByUserIdAndCreatedAtBeforeOrderByCreatedAtDescIdDesc(eq(user.getId()), eq(before), any()))
                .thenReturn(List.of(notification(true)));
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDescIdDesc(eq(user.getId()), any()))
                .thenReturn(List.of(notification(false)));
        when(notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtBeforeOrderByCreatedAtDescIdDesc(
                eq(user.getId()), eq(before), any())).thenReturn(List.of());

        assertThat(notificationService.getMyNotifications(user.getId(), before, false, null).items()).hasSize(1);
        assertThat(notificationService.getMyNotifications(user.getId(), null, true, 0).items()).hasSize(1);
        assertThat(notificationService.getMyNotifications(user.getId(), before, true, 500).items()).isEmpty();
    }

    @Test
    @DisplayName("лічильник непрочитаних")
    void countUnread() {
        when(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).thenReturn(4L);
        assertThat(notificationService.countUnread(user.getId())).isEqualTo(4);
    }

    @Test
    @DisplayName("позначення прочитаним оновлює лічильник у вкладках")
    void markRead() {
        Notification n = notification(false);
        when(notificationRepository.findWithUserById(n.getId())).thenReturn(Optional.of(n));
        when(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).thenReturn(2L);

        notificationService.markRead(n.getId(), user.getId());

        assertThat(n.getIsRead()).isTrue();
        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).sendTo(eq(user.getId()), event.capture());
        assertThat(event.getValue().kind()).isEqualTo(NotificationEvent.Kind.READ);
        assertThat(event.getValue().unreadCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("уже прочитане сповіщення не змінюється")
    void markReadTwice() {
        Notification n = notification(true);
        when(notificationRepository.findWithUserById(n.getId())).thenReturn(Optional.of(n));
        notificationService.markRead(n.getId(), user.getId());
        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("чуже або неіснуюче сповіщення")
    void markReadErrors() {
        Notification n = notification(false);
        when(notificationRepository.findWithUserById(n.getId())).thenReturn(Optional.of(n));
        assertThatThrownBy(() -> notificationService.markRead(n.getId(), UUID.randomUUID()))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> notificationService.markRead(UUID.randomUUID(), user.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("позначити всі прочитаними")
    void markAllRead() {
        when(notificationRepository.markAllRead(user.getId())).thenReturn(0, 3);
        notificationService.markAllRead(user.getId());
        verify(eventPublisher, never()).sendTo(any(), any());
        notificationService.markAllRead(user.getId());
        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).sendTo(eq(user.getId()), event.capture());
        assertThat(event.getValue().kind()).isEqualTo(NotificationEvent.Kind.READ_ALL);
    }

    @Test
    @DisplayName("нове сповіщення зберігається й надсилається в реальному часі")
    void notifyUser() {
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });
        when(notificationRepository.countByUserIdAndIsReadFalse(user.getId())).thenReturn(1L);
        when(linkResolver.resolve(any(Notification.class))).thenReturn("/exchanges");

        notificationService.notify(user, NotificationType.BOOK_SHIPPED, UUID.randomUUID(), "  Книгу відправлено  ");

        ArgumentCaptor<NotificationEvent> event = ArgumentCaptor.forClass(NotificationEvent.class);
        verify(eventPublisher).sendTo(eq(user.getId()), event.capture());
        assertThat(event.getValue().kind()).isEqualTo(NotificationEvent.Kind.CREATED);
        assertThat(event.getValue().notification().message()).isEqualTo("Книгу відправлено");
        assertThat(event.getValue().notification().link()).isEqualTo("/exchanges");
        assertThat(event.getValue().notification().createdAt()).isNotNull();
        assertThat(event.getValue().unreadCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("задовгий текст обрізається, порожній замінюється на рядок без символів")
    void notifyTrimsText() {
        ArgumentCaptor<Notification> saved = ArgumentCaptor.forClass(Notification.class);
        when(notificationRepository.save(saved.capture())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.notify(user, NotificationType.NEW_COMMENT, null, "а".repeat(600));
        notificationService.notify(user, NotificationType.NEW_COMMENT, null, null);

        assertThat(saved.getAllValues().get(0).getMessage()).hasSize(500).endsWith("…");
        assertThat(saved.getAllValues().get(1).getMessage()).isEmpty();
    }

    @Test
    @DisplayName("видалення старих прочитаних сповіщень")
    void deleteOld() {
        when(notificationRepository.deleteReadOlderThan(any())).thenReturn(6);
        assertThat(notificationService.deleteReadOlderThan(90)).isEqualTo(6);
    }
}
