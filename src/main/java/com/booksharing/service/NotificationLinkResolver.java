package com.booksharing.service;

import com.booksharing.entity.Notification;
import com.booksharing.enums.NotificationType;
import com.booksharing.repository.NotificationRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationLinkResolver {

    private static final Set<NotificationType> EXCHANGE_TYPES = Set.of(
            NotificationType.HANDOVER_READY,
            NotificationType.SHIPMENT_ADDRESS_ADDED,
            NotificationType.BOOK_SHIPPED,
            NotificationType.BOOK_RECEIVED,
            NotificationType.RETURN_STARTED,
            NotificationType.EXCHANGE_COMPLETED,
            NotificationType.DEADLINE_SOON,
            NotificationType.DEADLINE_OVERDUE,
            NotificationType.EXTENSION_REQUESTED,
            NotificationType.EXTENSION_DECIDED);

    private static final Set<NotificationType> DISPUTE_TYPES = Set.of(
            NotificationType.DISPUTE_FILED,
            NotificationType.DISPUTE_RESOLVED);

    private final NotificationRepository notificationRepository;

    public Map<UUID, String> resolve(Collection<Notification> notifications) {
        Set<UUID> approvedRequestIds = referenceIds(notifications, Set.of(NotificationType.REQUEST_APPROVED));
        Set<UUID> disputeIds = referenceIds(notifications, DISPUTE_TYPES);

        Map<UUID, UUID> exchangeByRequest = approvedRequestIds.isEmpty()
                ? Map.of()
                : toMap(notificationRepository.findExchangeIdsByRequestIds(approvedRequestIds));
        Map<UUID, UUID> exchangeByDispute = disputeIds.isEmpty()
                ? Map.of()
                : toMap(notificationRepository.findExchangeIdsByDisputeIds(disputeIds));

        Map<UUID, String> links = new HashMap<>();
        for (Notification n : notifications) {
            links.put(n.getId(), linkFor(n, exchangeByRequest, exchangeByDispute));
        }
        return links;
    }

    public String resolve(Notification notification) {
        return resolve(List.of(notification)).get(notification.getId());
    }

    private static String linkFor(
            Notification n, Map<UUID, UUID> exchangeByRequest, Map<UUID, UUID> exchangeByDispute) {
        UUID ref = n.getReferenceId();
        NotificationType type = n.getType();

        if (EXCHANGE_TYPES.contains(type)) {
            return ref == null ? "/exchanges" : "/exchanges/" + ref;
        }
        if (DISPUTE_TYPES.contains(type)) {
            UUID exchangeId = ref == null ? null : exchangeByDispute.get(ref);
            return exchangeId == null ? "/exchanges" : "/exchanges/" + exchangeId;
        }
        return switch (type) {
            case NEW_REQUEST -> "/requests";
            case REQUEST_REJECTED -> "/requests?tab=sent";
            case REQUEST_APPROVED -> {
                UUID exchangeId = ref == null ? null : exchangeByRequest.get(ref);
                yield exchangeId == null ? "/exchanges" : "/exchanges/" + exchangeId;
            }
            case NEW_COMMENT -> "/feed";
            case REVIEW_RECEIVED -> "/profile";
            case FEEDBACK_RECEIVED -> "/moderation?tab=feedback";
            case LISTING_HIDDEN, LISTING_RESTORED -> ref == null ? "/?tab=my" : "/listings/" + ref;
            case CONTENT_REMOVED -> "/feed";
            case ACCOUNT_RESTRICTED, ACCOUNT_RESTORED, MODERATOR_REVOKED -> "/profile";
            case MODERATOR_GRANTED -> "/moderation";
            case NEW_CHAT_MESSAGE -> ref == null ? "/chat" : "/chat/" + ref;
            default -> null;
        };
    }

    private static Set<UUID> referenceIds(Collection<Notification> notifications, Set<NotificationType> types) {
        return notifications.stream()
                .filter(n -> types.contains(n.getType()) && n.getReferenceId() != null)
                .map(Notification::getReferenceId)
                .collect(Collectors.toSet());
    }

    private static Map<UUID, UUID> toMap(List<Object[]> rows) {
        Map<UUID, UUID> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((UUID) row[0], (UUID) row[1]);
        }
        return map;
    }
}