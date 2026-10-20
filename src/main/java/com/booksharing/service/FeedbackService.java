package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.CreateFeedbackRequest;
import com.booksharing.dto.req.UpdateFeedbackRequest;
import com.booksharing.dto.res.FeedbackListResponse;
import com.booksharing.dto.res.FeedbackResponse;
import com.booksharing.entity.Feedback;
import com.booksharing.entity.User;
import com.booksharing.enums.FeedbackStatus;
import com.booksharing.enums.FeedbackTopic;
import com.booksharing.enums.NotificationType;
import com.booksharing.enums.UserRole;
import com.booksharing.repository.FeedbackRepository;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final int MAX_PER_HOUR = 5;

    private final FeedbackRepository feedbackRepository;
    private final NotificationService notificationService;

    @Value("${app.backend.base-url}")
    private String backendBaseUrl;

    @Transactional
    public FeedbackResponse create(CreateFeedbackRequest request, User currentUser, String ipAddress) {
        String email = request.email().trim();
        LocalDateTime hourAgo = LocalDateTime.now().minusHours(1);
        boolean tooManyFromIp = ipAddress != null
                && feedbackRepository.countByIpAddressAndCreatedAtAfter(ipAddress, hourAgo) >= MAX_PER_HOUR;
        boolean tooManyFromEmail = feedbackRepository.countByEmailSince(email, hourAgo) >= MAX_PER_HOUR;
        if (tooManyFromIp || tooManyFromEmail) {
            throw new IllegalStateException("Забагато звернень за останню годину. Спробуйте трохи пізніше.");
        }

        String link = blankToNull(request.link());
        if (link != null && !link.matches("(?i)^https?://.+")) {
            throw new IllegalArgumentException("Посилання має починатися з http:// або https://");
        }

        String screenshotUrl = blankToNull(request.screenshotUrl());
        if (screenshotUrl != null) {
            if (currentUser == null) {
                throw new IllegalArgumentException("Додати скріншот можуть лише користувачі, що увійшли");
            }
            if (!screenshotUrl.startsWith(backendBaseUrl + "/uploads/")) {
                throw new IllegalArgumentException("Скріншот має бути завантажений через сервіс");
            }
        }

        Feedback feedback = feedbackRepository.save(Feedback.builder()
                .topic(request.topic())
                .email(email)
                .message(request.message().trim())
                .link(link)
                .screenshotUrl(screenshotUrl)
                .user(currentUser)
                .ipAddress(ipAddress)
                .status(FeedbackStatus.NEW)
                .build());

        String text = topicLabel(request.topic()) + " від "
                + (currentUser != null ? currentUser.getName() : email);
        feedbackRepository.findUsersByRoles(List.of(UserRole.MODERATOR, UserRole.ADMIN))
                .forEach(moderator -> notificationService.notify(
                        moderator, NotificationType.FEEDBACK_RECEIVED, feedback.getId(), text));

        return toResponse(feedback);
    }

    @Transactional(readOnly = true)
    public FeedbackListResponse search(FeedbackStatus status, FeedbackTopic topic, int page, int size) {
        Page<Feedback> result = feedbackRepository.search(
                status, topic,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));

        Map<FeedbackStatus, Long> counts = new EnumMap<>(FeedbackStatus.class);
        for (FeedbackStatus s : FeedbackStatus.values()) {
            counts.put(s, feedbackRepository.countByStatus(s));
        }

        return new FeedbackListResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                counts,
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements());
    }

    @Transactional
    public FeedbackResponse update(UUID id, UpdateFeedbackRequest request, User moderator) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Звернення не знайдено: " + id));
        if (request.status() != null) {
            feedback.setStatus(request.status());
        }
        if (request.moderatorNote() != null) {
            feedback.setModeratorNote(blankToNull(request.moderatorNote()));
        }
        feedback.setHandledBy(moderator);
        return toResponse(feedbackRepository.save(feedback));
    }

    private FeedbackResponse toResponse(Feedback f) {
        User user = f.getUser();
        User handledBy = f.getHandledBy();
        return new FeedbackResponse(
                f.getId(),
                f.getTopic(),
                f.getEmail(),
                f.getMessage(),
                f.getLink(),
                f.getScreenshotUrl(),
                user != null ? user.getId() : null,
                user != null ? user.getName() : null,
                user != null ? user.getAvatarUrl() : null,
                f.getStatus(),
                f.getModeratorNote(),
                handledBy != null ? handledBy.getName() : null,
                f.getCreatedAt(),
                f.getUpdatedAt());
    }

    private static String topicLabel(FeedbackTopic topic) {
        return switch (topic) {
            case QUESTION -> "Нове питання";
            case BUG -> "Повідомлення про проблему";
            case IDEA -> "Нова пропозиція";
            case COMPLAINT -> "Нова скарга";
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}