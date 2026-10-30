package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.booksharing.support.TestData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Звернення користувачів")
class FeedbackServiceTest {

    private static final String BASE_URL = "http://localhost:8080";

    @Mock private FeedbackRepository feedbackRepository;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private FeedbackService feedbackService;

    private User user;
    private User moderator;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(feedbackService, "backendBaseUrl", BASE_URL);
        user = TestData.user("Користувач");
        moderator = TestData.user("Модератор", UserRole.MODERATOR);
        when(feedbackRepository.save(any(Feedback.class))).thenAnswer(inv -> {
            Feedback f = inv.getArgument(0);
            if (f.getId() == null) {
                f.setId(UUID.randomUUID());
            }
            return f;
        });
        when(feedbackRepository.findUsersByRoles(any())).thenReturn(List.of(moderator));
    }

    private CreateFeedbackRequest request(FeedbackTopic topic, String link, String screenshot) {
        return new CreateFeedbackRequest(topic, " user@test.ua ", "  Не завантажується фото  ", link, screenshot);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({"QUESTION, Нове питання", "BUG, Повідомлення про проблему", "IDEA, Нова пропозиція", "COMPLAINT, Нова скарга"})
    @DisplayName("звернення сповіщає модераторів з назвою теми")
    void notifiesModerators(FeedbackTopic topic, String label) {
        FeedbackResponse response = feedbackService.create(request(topic, null, null), null, "10.0.0.1");

        assertThat(response.email()).isEqualTo("user@test.ua");
        assertThat(response.message()).isEqualTo("Не завантажується фото");
        assertThat(response.status()).isEqualTo(FeedbackStatus.NEW);
        assertThat(response.userId()).isNull();
        verify(notificationService).notify(eq(moderator), eq(NotificationType.FEEDBACK_RECEIVED), any(),
                eq(label + " від user@test.ua"));
    }

    @Test
    @DisplayName("звернення від користувача зі скріншотом і посиланням")
    void fromUserWithScreenshot() {
        FeedbackResponse response = feedbackService.create(
                request(FeedbackTopic.BUG, " https://bookshare.ua/listings ", BASE_URL + "/uploads/s.png"), user, null);

        assertThat(response.link()).isEqualTo("https://bookshare.ua/listings");
        assertThat(response.screenshotUrl()).isEqualTo(BASE_URL + "/uploads/s.png");
        assertThat(response.userName()).isEqualTo("Користувач");
        verify(notificationService).notify(eq(moderator), any(), any(), eq("Повідомлення про проблему від Користувач"));
    }

    @Test
    @DisplayName("посилання має бути http або https")
    void badLink() {
        assertThatThrownBy(() -> feedbackService.create(request(FeedbackTopic.BUG, "ftp://x", null), user, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("гість не може додати скріншот")
    void guestScreenshot() {
        assertThatThrownBy(() -> feedbackService.create(
                request(FeedbackTopic.BUG, null, BASE_URL + "/uploads/s.png"), null, "10.0.0.1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("увійшли");
    }

    @Test
    @DisplayName("скріншот з чужого сервера відхиляється")
    void foreignScreenshot() {
        assertThatThrownBy(() -> feedbackService.create(
                request(FeedbackTopic.BUG, null, "https://evil.com/s.png"), user, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("не більше п'яти звернень на годину з однієї адреси")
    void rateLimitByIp() {
        when(feedbackRepository.countByIpAddressAndCreatedAtAfter(eq("10.0.0.1"), any())).thenReturn(5L);
        assertThatThrownBy(() -> feedbackService.create(request(FeedbackTopic.IDEA, null, null), null, "10.0.0.1"))
                .isInstanceOf(IllegalStateException.class);
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    @DisplayName("не більше п'яти звернень на годину з однієї пошти")
    void rateLimitByEmail() {
        when(feedbackRepository.countByEmailSince(eq("user@test.ua"), any())).thenReturn(5L);
        assertThatThrownBy(() -> feedbackService.create(request(FeedbackTopic.IDEA, null, null), user, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("пошук із лічильниками за статусами")
    void search() {
        Feedback f = Feedback.builder().id(UUID.randomUUID()).topic(FeedbackTopic.QUESTION).email("a@b.c")
                .message("?").user(user).handledBy(moderator).status(FeedbackStatus.IN_PROGRESS).build();
        when(feedbackRepository.search(eq(FeedbackStatus.IN_PROGRESS), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f)));
        when(feedbackRepository.countByStatus(any())).thenReturn(1L);

        FeedbackListResponse result = feedbackService.search(FeedbackStatus.IN_PROGRESS, null, -1, 1000);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).handledByName()).isEqualTo("Модератор");
        assertThat(result.statusCounts()).containsEntry(FeedbackStatus.NEW, 1L).hasSize(3);
    }

    @Test
    @DisplayName("модератор змінює статус і нотатку")
    void update() {
        Feedback f = Feedback.builder().id(UUID.randomUUID()).topic(FeedbackTopic.BUG).email("a@b.c").message("!").build();
        when(feedbackRepository.findById(f.getId())).thenReturn(Optional.of(f));

        FeedbackResponse response = feedbackService.update(f.getId(),
                new UpdateFeedbackRequest(FeedbackStatus.RESOLVED, "  Виправлено  "), moderator);

        assertThat(response.status()).isEqualTo(FeedbackStatus.RESOLVED);
        assertThat(response.moderatorNote()).isEqualTo("Виправлено");
        assertThat(response.handledByName()).isEqualTo("Модератор");

        feedbackService.update(f.getId(), new UpdateFeedbackRequest(null, " "), moderator);
        assertThat(f.getStatus()).isEqualTo(FeedbackStatus.RESOLVED);
        assertThat(f.getModeratorNote()).isNull();

        feedbackService.update(f.getId(), new UpdateFeedbackRequest(null, null), moderator);
        assertThat(f.getModeratorNote()).isNull();
    }

    @Test
    @DisplayName("неіснуюче звернення")
    void updateMissing() {
        assertThatThrownBy(() -> feedbackService.update(UUID.randomUUID(), new UpdateFeedbackRequest(null, null), moderator))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
