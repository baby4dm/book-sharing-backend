package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.res.NotificationResponse;
import com.booksharing.entity.Notification;
import com.booksharing.enums.NotificationType;
import com.booksharing.mapper.NotificationMapper;
import com.booksharing.entity.User;
import java.util.List;
import java.util.UUID;

import com.booksharing.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link #notify} - внутрішній метод, яким користуються ІНШІ сервіси
 * (Request, Exchange, Dispute, Chat, Feed), а не REST-контролер напряму.
 * Немає окремого DTO для створення сповіщення ззовні - користувач не
 * може створювати сповіщення сам, лише системні події це роблять.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;

    public List<NotificationResponse> getMyNotifications(UUID userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(notificationMapper::toResponse)
                .toList();
    }

    @Transactional
    public void markRead(UUID notificationId, UUID currentUserId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Сповіщення не знайдено: " + notificationId));
        if (!notification.getUser().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Це не ваше сповіщення");
        }
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notify(User user, NotificationType type, UUID referenceId, String message) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .referenceId(referenceId)
                .message(message)
                .build());
    }
}