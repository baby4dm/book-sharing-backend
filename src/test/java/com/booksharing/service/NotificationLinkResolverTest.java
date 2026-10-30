package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.entity.Notification;
import com.booksharing.enums.NotificationType;
import com.booksharing.repository.NotificationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Посилання зі сповіщень")
class NotificationLinkResolverTest {

    private static final UUID REF = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationLinkResolver resolver;

    private static Notification notification(NotificationType type, UUID ref) {
        return Notification.builder().id(UUID.randomUUID()).type(type).referenceId(ref).build();
    }

    private static List<Object[]> rows(Object[]... rows) {
        return new ArrayList<>(List.of(rows));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "HANDOVER_READY, /exchanges/11111111-1111-1111-1111-111111111111",
            "DEADLINE_OVERDUE, /exchanges/11111111-1111-1111-1111-111111111111",
            "NEW_REQUEST, /requests",
            "REQUEST_REJECTED, /requests?tab=sent",
            "NEW_COMMENT, /feed",
            "CONTENT_REMOVED, /feed",
            "REVIEW_RECEIVED, /profile",
            "ACCOUNT_RESTRICTED, /profile",
            "ACCOUNT_RESTORED, /profile",
            "MODERATOR_REVOKED, /profile",
            "MODERATOR_GRANTED, /moderation",
            "FEEDBACK_RECEIVED, /moderation?tab=feedback",
            "LISTING_HIDDEN, /listings/11111111-1111-1111-1111-111111111111",
            "LISTING_RESTORED, /listings/11111111-1111-1111-1111-111111111111",
            "NEW_CHAT_MESSAGE, /chat/11111111-1111-1111-1111-111111111111"
    })
    @DisplayName("посилання за типом сповіщення")
    void linkByType(NotificationType type, String expected) {
        assertThat(resolver.resolve(notification(type, REF))).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource({
            "BOOK_RECEIVED, /exchanges",
            "LISTING_HIDDEN, /?tab=my",
            "NEW_CHAT_MESSAGE, /chat",
            "REQUEST_APPROVED, /exchanges",
            "DISPUTE_FILED, /exchanges"
    })
    @DisplayName("посилання без id об'єкта")
    void linkWithoutReference(NotificationType type, String expected) {
        assertThat(resolver.resolve(notification(type, null))).isEqualTo(expected);
        verify(notificationRepository, never()).findExchangeIdsByRequestIds(any());
        verify(notificationRepository, never()).findExchangeIdsByDisputeIds(any());
    }

    @Test
    @DisplayName("схвалена заявка й скарга ведуть на свій обмін")
    void requestAndDisputeLeadToExchange() {
        UUID requestId = UUID.randomUUID();
        UUID disputeId = UUID.randomUUID();
        UUID exchangeA = UUID.randomUUID();
        UUID exchangeB = UUID.randomUUID();
        when(notificationRepository.findExchangeIdsByRequestIds(any())).thenReturn(rows(new Object[]{requestId, exchangeA}));
        when(notificationRepository.findExchangeIdsByDisputeIds(any())).thenReturn(rows(new Object[]{disputeId, exchangeB}));
        Notification approved = notification(NotificationType.REQUEST_APPROVED, requestId);
        Notification dispute = notification(NotificationType.DISPUTE_RESOLVED, disputeId);
        Notification unknownDispute = notification(NotificationType.DISPUTE_FILED, UUID.randomUUID());

        Map<UUID, String> links = resolver.resolve(List.of(approved, dispute, unknownDispute));

        assertThat(links).containsEntry(approved.getId(), "/exchanges/" + exchangeA)
                .containsEntry(dispute.getId(), "/exchanges/" + exchangeB)
                .containsEntry(unknownDispute.getId(), "/exchanges");
    }

    @Test
    @DisplayName("для застарілих типів посилання немає")
    void noLinkForOtherTypes() {
        assertThat(resolver.resolve(notification(NotificationType.EXTENSION_REQUESTED, null))).isEqualTo("/exchanges");
        assertThat(resolver.resolve(List.of())).isEmpty();
    }
}
