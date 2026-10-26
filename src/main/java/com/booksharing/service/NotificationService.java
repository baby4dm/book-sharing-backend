package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.res.NotificationEvent;
import com.booksharing.dto.res.NotificationPageResponse;
import com.booksharing.dto.res.NotificationResponse;
import com.booksharing.entity.Notification;
import com.booksharing.entity.User;
import com.booksharing.enums.NotificationType;
import com.booksharing.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final int MESSAGE_MAX = 500;

    private final NotificationRepository notificationRepository;
    private final NotificationLinkResolver linkResolver;
    private final NotificationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public NotificationPageResponse getMyNotifications(
            UUID userId, LocalDateTime before, boolean unreadOnly, Integer size) {
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        // на одне більше — щоб знати, чи є ще старіші
        PageRequest page = PageRequest.of(0, pageSize + 1);

        List<Notification> rows;
        if (unreadOnly) {
            rows = before == null
                    ? notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDescIdDesc(userId, page)
                    : notificationRepository.findByUserIdAndIsReadFalseAndCreatedAtBeforeOrderByCreatedAtDescIdDesc(
                    userId, before, page);
        } else {
            rows = before == null
                    ? notificationRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, page)
                    : notificationRepository.findByUserIdAndCreatedAtBeforeOrderByCreatedAtDescIdDesc(
                    userId, before, page);
        }

        boolean hasMore = rows.size() > pageSize;
        List<Notification> slice = hasMore ? rows.subList(0, pageSize) : rows;
        Map<UUID, String> links = linkResolver.resolve(slice);

        return new NotificationPageResponse(
                slice.stream().map(n -> toResponse(n, links.get(n.getId()))).toList(),
                hasMore);
    }

    @Transactional(readOnly = true)
    public long countUnread(UUID userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(UUID notificationId, UUID currentUserId) {
        Notification notification = notificationRepository.findWithUserById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Сповіщення не знайдено: " + notificationId));
        if (!notification.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Це не ваше сповіщення");
        }
        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return;
        }
        notification.setIsRead(true);
        notificationRepository.saveAndFlush(notification);

        eventPublisher.sendTo(currentUserId, NotificationEvent.read(
                notificationId, notificationRepository.countByUserIdAndIsReadFalse(currentUserId)));
    }

    @Transactional
    public void markAllRead(UUID currentUserId) {
        if (notificationRepository.markAllRead(currentUserId) > 0) {
            eventPublisher.sendTo(currentUserId, NotificationEvent.readAll());
        }
    }

    @Transactional
    public void notify(User user, NotificationType type, UUID referenceId, String message) {
        String text = message == null ? "" : message.strip();
        if (text.length() > MESSAGE_MAX) {
            text = text.substring(0, MESSAGE_MAX - 1) + "…";
        }

        Notification notification = notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .referenceId(referenceId)
                .message(text)
                .build());

        long unread = notificationRepository.countByUserIdAndIsReadFalse(user.getId());
        NotificationResponse response = toResponse(notification, linkResolver.resolve(notification));
        eventPublisher.sendTo(user.getId(), NotificationEvent.created(response, unread));
    }

    @Transactional
    public int deleteReadOlderThan(int days) {
        return notificationRepository.deleteReadOlderThan(LocalDateTime.now().minusDays(days));
    }

    private static NotificationResponse toResponse(Notification n, String link) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getReferenceId(),
                n.getMessage(),
                Boolean.TRUE.equals(n.getIsRead()),
                // @CreationTimestamp заповнюється при flush; до нього — «зараз»
                n.getCreatedAt() != null ? n.getCreatedAt() : LocalDateTime.now(),
                link);
    }
}