package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.*;
import com.booksharing.dto.res.*;
import com.booksharing.entity.*;
import com.booksharing.entity.Listing;
import com.booksharing.enums.*;
import com.booksharing.repository.ListingRepository;
import com.booksharing.entity.User;
import com.booksharing.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import com.booksharing.repository.DeadlineExtensionRequestRepository;
import com.booksharing.repository.DisputeRepository;
import com.booksharing.repository.ExchangePhotoRepository;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.ReviewRepository;
import com.booksharing.repository.ShipmentInfoRepository;
import com.booksharing.spec.ExchangeSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final ExchangeRepository exchangeRepository;
    private final ExchangePhotoRepository exchangePhotoRepository;
    private final ShipmentInfoRepository shipmentInfoRepository;
    private final DeadlineExtensionRequestRepository extensionRequestRepository;
    private final ReviewRepository reviewRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ExchangeNotifier exchangeNotifier;
    private final DisputeRepository disputeRepository;
    private final UserStatsService userStatsService;

    @Transactional(readOnly = true)
    public ExchangeResponse getById(UUID id) {
        return toResponse(findExchangeOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<ExchangeResponse> getMyExchanges(UUID userId) {
        List<Exchange> asOwner = exchangeRepository.findByOwnerId(userId);
        List<Exchange> asReader = exchangeRepository.findByReaderId(userId);
        return java.util.stream.Stream.concat(asOwner.stream(), asReader.stream())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ExchangeResponse submitHandoverPhotos(UUID exchangeId, UUID currentUserId, SubmitPhotosRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireOwner(exchange, currentUserId);
        requireStatus(exchange, ExchangeStatus.HANDOVER_PENDING,
                "Фото стану книги можна надсилати лише до передачі");

        if (hasPhotoBy(exchangeId, PhotoStage.HANDOVER, exchange.getOwner().getId())) {
            throw new IllegalStateException("Ви вже надіслали фото стану книги");
        }

        savePhotos(exchange, exchange.getOwner(), PhotoStage.HANDOVER, request.urls());
        exchangeNotifier.handoverReady(exchange);
        return toResponse(exchange);
    }

    @Transactional
    public ExchangeResponse confirmReceived(UUID exchangeId, UUID currentUserId, SubmitPhotosRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireReader(exchange, currentUserId);
        requireStatus(exchange, ExchangeStatus.HANDOVER_PENDING,
                "Обмін не очікує підтвердження отримання");

        if (!hasPhotoBy(exchangeId, PhotoStage.HANDOVER, exchange.getOwner().getId())) {
            throw new IllegalStateException("Власник ще не надіслав фото стану книги");
        }

        if (exchange.getDeliveryMethod() == DeliveryMethod.MAIL) {
            markDelivered(requireShippedShipment(
                    exchangeId, ShipmentDirection.TO_READER, "Власник ще не відправив книгу"));
        }

        savePhotos(exchange, exchange.getReader(), PhotoStage.HANDOVER, request.urls());

        exchange.setReadingStartedAt(LocalDateTime.now());
        exchange.setDeadline(LocalDate.now().plusDays(exchange.getReadingDays()));
        exchange.setStatus(ExchangeStatus.IN_READING);
        exchangeRepository.save(exchange);
        exchangeNotifier.bookReceived(exchange);

        Listing listing = exchange.getListing();
        listing.setStatus(ListingStatus.IN_EXCHANGE);
        listingRepository.save(listing);

        return toResponse(exchange);
    }

    @Transactional
    public ExchangeResponse submitReturnPhotos(UUID exchangeId, UUID currentUserId, SubmitPhotosRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireReader(exchange, currentUserId);
        if (exchange.getStatus() != ExchangeStatus.IN_READING
                && exchange.getStatus() != ExchangeStatus.OVERDUE) {
            throw new IllegalStateException("Фото повернення можна надіслати лише під час читання");
        }

        savePhotos(exchange, exchange.getReader(), PhotoStage.RETURN, request.urls());

        LocalDateTime now = LocalDateTime.now();
        exchange.setReturnPreparedAt(now);
        if (exchange.getDeliveryMethod() != DeliveryMethod.MAIL) {
            exchange.setReturnedAt(now);
        }
        exchange.setStatus(ExchangeStatus.RETURN_PENDING);
        exchangeRepository.save(exchange);
        exchangeNotifier.returnStarted(exchange);

        return toResponse(exchange);
    }

    @Transactional(readOnly = true)
    public ExchangeListResponse searchMyExchanges(
            UUID userId,
            String search,
            ExchangeRole role,
            DeliveryMethod deliveryMethod,
            ExchangeStatus status,
            ExchangeSort sort,
            int page,
            int size) {
        Specification<Exchange> base = Specification.allOf(
                ExchangeSpecifications.involving(userId, role),
                ExchangeSpecifications.hasDeliveryMethod(deliveryMethod),
                ExchangeSpecifications.matchesSearch(search, userId));

        Map<ExchangeStatus, Long> counts = new EnumMap<>(ExchangeStatus.class);
        for (ExchangeStatus value : ExchangeStatus.values()) {
            counts.put(value, exchangeRepository.count(
                    base.and(ExchangeSpecifications.hasStatus(value))));
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        Specification<Exchange> filtered = base
                .and(ExchangeSpecifications.hasStatus(status))
                .and(ExchangeSpecifications.sortedBy(sort));

        Page<Exchange> result = exchangeRepository.findAll(
                filtered,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50)));

        return new ExchangeListResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                counts,
                total,
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements());
    }

    @Transactional
    public ExchangeResponse confirmReturn(UUID exchangeId, UUID currentUserId) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireOwner(exchange, currentUserId);
        requireStatus(exchange, ExchangeStatus.RETURN_PENDING,
                "Обмін не очікує підтвердження повернення");

        if (!hasPhotoBy(exchangeId, PhotoStage.RETURN, exchange.getReader().getId())) {
            throw new IllegalStateException("Читач ще не надіслав фото стану книги перед поверненням");
        }

        if (exchange.getDeliveryMethod() == DeliveryMethod.MAIL) {
            markDelivered(requireShippedShipment(
                    exchangeId, ShipmentDirection.TO_OWNER, "Читач ще не відправив книгу назад"));
        }

        finalizeReturnedExchange(exchange);

        return toResponse(exchange);
    }

    @Transactional
    public void finalizeReturnedExchange(Exchange exchange) {
        exchange.setStatus(ExchangeStatus.COMPLETED);
        if (exchange.getCompletedAt() == null) {
            exchange.setCompletedAt(LocalDateTime.now());
        }
        exchangeRepository.save(exchange);

        Listing listing = exchange.getListing();
        listing.setStatus(ListingStatus.AVAILABLE);
        listingRepository.save(listing);

        User owner = exchange.getOwner();
        owner.setBooksGiven(owner.getBooksGiven() + 1);
        userRepository.save(owner);

        User reader = exchange.getReader();
        reader.setBooksTaken(reader.getBooksTaken() + 1);
        LocalDate dueDate = exchange.getDueDate();
        LocalDate returnedOn = exchange.getReturnedAt() != null
                ? exchange.getReturnedAt().toLocalDate()
                : LocalDate.now();
        if (dueDate == null || !returnedOn.isAfter(dueDate)) {
            reader.setBooksReturnedOnTime(reader.getBooksReturnedOnTime() + 1);
        } else {
            reader.setBooksOverdue(reader.getBooksOverdue() + 1);
        }
        userRepository.save(reader);

        exchangeNotifier.completed(exchange);
    }

    @Transactional
    public ExchangeResponse addDisputePhoto(UUID exchangeId, UUID currentUserId, AddExchangePhotoRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        User uploader = requireParticipant(exchange, currentUserId);

        if (exchange.getStatus() != ExchangeStatus.RETURN_PENDING) {
            throw new IllegalStateException("Фото-доказ для спору можна додавати лише на етапі очікування повернення");
        }

        exchangePhotoRepository.save(ExchangePhoto.builder()
                .exchange(exchange)
                .uploadedBy(uploader)
                .stage(PhotoStage.DISPUTE)
                .url(request.url())
                .note(request.note())
                .build());

        return toResponse(exchange);
    }

    @Transactional
    public ExchangeResponse openDispute(UUID exchangeId, UUID currentUserId, OpenDisputeRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireOwner(exchange, currentUserId);
        requireStatus(exchange, ExchangeStatus.RETURN_PENDING,
                "Спір можна відкрити лише на етапі очікування повернення");

        boolean alreadyOpen = disputeRepository.findByExchangeId(exchangeId).stream()
                .anyMatch(d -> d.getStatus() == DisputeStatus.OPEN);
        if (alreadyOpen) {
            throw new IllegalStateException("По цьому обміну вже є відкритий спір");
        }

        savePhotos(exchange, exchange.getOwner(), PhotoStage.DISPUTE, request.urls());

        exchange.setStatus(ExchangeStatus.DISPUTED);
        exchange.setDisputeReason(request.reason());
        exchangeRepository.save(exchange);

        // Запис спору — саме його бачить і розглядає модератор
        Dispute dispute = disputeRepository.save(Dispute.builder()
                .exchange(exchange)
                .filedBy(exchange.getOwner())
                .description(request.reason())
                .status(DisputeStatus.OPEN)
                .build());

        notificationService.notify(
                exchange.getReader(),
                NotificationType.DISPUTE_FILED,
                dispute.getId(),
                exchange.getOwner().getName() + " відкрив(-ла) спір щодо повернення «"
                        + exchange.getListing().getBookCatalogEntry().getTitle() + "»");

        return toResponse(exchange);
    }

    @Transactional
    public ExchangeResponse requestExtension(UUID exchangeId, UUID currentUserId, CreateExtensionRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireReader(exchange, currentUserId);

        if (exchange.getStatus() != ExchangeStatus.IN_READING
                && exchange.getStatus() != ExchangeStatus.OVERDUE) {
            throw new IllegalStateException("Продовження дедлайну можна запросити лише під час читання");
        }
        if (exchange.getDeadline() == null) {
            throw new IllegalStateException("Термін читання ще не почався");
        }
        boolean hasPending = extensionRequestRepository.findByExchangeId(exchangeId).stream()
                .anyMatch(e -> e.getStatus() == ExtensionStatus.PENDING);
        if (hasPending) {
            throw new IllegalStateException("Попередній запит на продовження ще не розглянуто");
        }

        extensionRequestRepository.save(DeadlineExtensionRequest.builder()
                .exchange(exchange)
                .extraDays(request.extraDays())
                .comment(request.comment())
                .status(ExtensionStatus.PENDING)
                .build());

        notificationService.notify(
                exchange.getOwner(),
                NotificationType.EXTENSION_REQUESTED,
                exchange.getId(),
                exchange.getReader().getName() + " просить продовжити читання «"
                        + exchange.getListing().getBookCatalogEntry().getTitle() + "» на "
                        + request.extraDays() + " дн.");

        return toResponse(exchange);
    }

    @Transactional
    public ExchangeResponse decideExtension(
            UUID exchangeId, UUID extensionRequestId, UUID currentUserId, boolean approve,
            String decisionComment) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireOwner(exchange, currentUserId);

        DeadlineExtensionRequest extension = extensionRequestRepository.findById(extensionRequestId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Запит на продовження не знайдено: " + extensionRequestId));
        if (!extension.getExchange().getId().equals(exchangeId)) {
            throw new IllegalArgumentException("Запит на продовження належить іншому обміну");
        }
        if (extension.getStatus() != ExtensionStatus.PENDING) {
            throw new IllegalStateException("Цей запит уже розглянуто");
        }

        extension.setStatus(approve ? ExtensionStatus.APPROVED : ExtensionStatus.REJECTED);
        extension.setDecidedAt(LocalDateTime.now());
        extension.setDecisionComment(
                decisionComment == null || decisionComment.isBlank() ? null : decisionComment.trim());
        extensionRequestRepository.save(extension);

        if (approve) {
            LocalDate base = exchange.getExtendedDeadline() != null
                    ? exchange.getExtendedDeadline()
                    : exchange.getDeadline();
            LocalDate newDeadline = base.plusDays(extension.getExtraDays());
            extension.setRequestedNewDeadline(newDeadline);
            extensionRequestRepository.save(extension);
            exchange.setExtendedDeadline(newDeadline);
            exchangeRepository.save(exchange);
        }

        exchangeNotifier.extensionDecided(exchange, approve);

        return toResponse(exchange);
    }

    @Transactional
    public ExchangeResponse createShipment(UUID exchangeId, UUID currentUserId, CreateShipmentRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);

        if (exchange.getDeliveryMethod() != DeliveryMethod.MAIL) {
            throw new IllegalStateException("Цей обмін не передбачає доставку поштою");
        }

        UUID expectedRecipientId = request.direction() == ShipmentDirection.TO_READER
                ? exchange.getReader().getId()
                : exchange.getOwner().getId();
        if (!expectedRecipientId.equals(currentUserId)) {
            throw new AccessDeniedException("Контактні дані для цього напрямку вказує інший учасник обміну");
        }

        if (request.direction() == ShipmentDirection.TO_READER) {
            requireStatus(exchange, ExchangeStatus.HANDOVER_PENDING,
                    "Адресу доставки до читача можна вказати лише до отримання книги");
            if (!hasPhotoBy(exchangeId, PhotoStage.HANDOVER, exchange.getOwner().getId())) {
                throw new IllegalStateException("Власник ще не надіслав фото стану книги");
            }
        } else {
            requireStatus(exchange, ExchangeStatus.RETURN_PENDING,
                    "Адресу для повернення можна вказати після того, як читач надішле фото повернення");
        }

        boolean alreadyExists = shipmentInfoRepository
                .findByExchangeIdAndDirection(exchangeId, request.direction())
                .isPresent();
        if (alreadyExists) {
            throw new IllegalStateException("Відправлення для цього напрямку вже створено");
        }

        if (request.direction() == ShipmentDirection.TO_OWNER && exchange.getReturnPreparedAt() != null) {
            long waitedDays = java.time.temporal.ChronoUnit.DAYS.between(
                    exchange.getReturnPreparedAt().toLocalDate(), LocalDate.now());
            if (waitedDays > 0) {
                exchange.setReadingPausedDays(exchange.getReadingPausedDays() + (int) waitedDays);
                exchangeRepository.save(exchange);
            }
        }

        shipmentInfoRepository.save(ShipmentInfo.builder()
                .exchange(exchange)
                .direction(request.direction())
                .recipientName(request.recipientName())
                .recipientPhone(request.recipientPhone())
                .carrier(request.carrier())
                .city(request.city())
                .branchNumber(request.branchNumber())
                .status(ShipmentStatus.PENDING)
                .build());

        exchangeNotifier.shipmentAddressAdded(exchange, request.direction());

        return toResponse(exchange);
    }

    @Transactional
    public ExchangeResponse shipWaybill(
            UUID exchangeId, UUID shipmentId, UUID currentUserId, ShipWaybillRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        ShipmentInfo shipment = findShipmentOrThrow(exchange, shipmentId);

        UUID expectedSenderId = shipment.getDirection() == ShipmentDirection.TO_READER
                ? exchange.getOwner().getId()
                : exchange.getReader().getId();
        if (!expectedSenderId.equals(currentUserId)) {
            throw new AccessDeniedException("Накладну для цього відправлення вантажить інший учасник обміну");
        }
        requireStatus(exchange,
                shipment.getDirection() == ShipmentDirection.TO_READER
                        ? ExchangeStatus.HANDOVER_PENDING
                        : ExchangeStatus.RETURN_PENDING,
                "Накладну не можна додати на поточному етапі обміну");
        if (shipment.getStatus() != ShipmentStatus.PENDING) {
            throw new IllegalStateException("Це відправлення вже позначено як відправлене");
        }

        shipment.setWaybillPhotoUrl(request.waybillPhotoUrl());
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setShippedAt(LocalDateTime.now());
        shipmentInfoRepository.save(shipment);

        if (shipment.getDirection() == ShipmentDirection.TO_OWNER) {
            exchange.setReturnedAt(shipment.getShippedAt());
            exchangeRepository.save(exchange);
        }

        exchangeNotifier.bookShipped(exchange, shipment);

        return toResponse(exchange);
    }


    private void requireStatus(Exchange exchange, ExchangeStatus expected, String message) {
        if (exchange.getStatus() != expected) {
            throw new IllegalStateException(message);
        }
    }

    private boolean hasPhotoBy(UUID exchangeId, PhotoStage stage, UUID userId) {
        return exchangePhotoRepository.findByExchangeId(exchangeId).stream()
                .anyMatch(p -> p.getStage() == stage && p.getUploadedBy().getId().equals(userId));
    }

    private void savePhotos(Exchange exchange, User uploader, PhotoStage stage, List<String> urls) {
        for (String url : urls) {
            exchangePhotoRepository.save(ExchangePhoto.builder()
                    .exchange(exchange)
                    .uploadedBy(uploader)
                    .stage(stage)
                    .url(url)
                    .build());
        }
    }

    private Optional<ShipmentInfo> findShipment(UUID exchangeId, ShipmentDirection direction) {
        return shipmentInfoRepository.findByExchangeIdAndDirection(exchangeId, direction);
    }

    private ShipmentInfo requireShippedShipment(UUID exchangeId, ShipmentDirection direction, String message) {
        return findShipment(exchangeId, direction)
                .filter(s -> s.getStatus() == ShipmentStatus.SHIPPED)
                .orElseThrow(() -> new IllegalStateException(message));
    }

    private void markDelivered(ShipmentInfo shipment) {
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(LocalDateTime.now());
        shipmentInfoRepository.save(shipment);
    }

    private User requireParticipant(Exchange exchange, UUID currentUserId) {
        if (exchange.getOwner().getId().equals(currentUserId)) {
            return exchange.getOwner();
        }
        if (exchange.getReader().getId().equals(currentUserId)) {
            return exchange.getReader();
        }
        throw new AccessDeniedException("Ви не берете участі в цьому обміні");
    }

    private void requireOwner(Exchange exchange, UUID currentUserId) {
        if (!exchange.getOwner().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Ця дія доступна лише власнику книги");
        }
    }

    private void requireReader(Exchange exchange, UUID currentUserId) {
        if (!exchange.getReader().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Ця дія доступна лише читачу");
        }
    }

    private Exchange findExchangeOrThrow(UUID id) {
        return exchangeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Обмін не знайдено: " + id));
    }

    private ShipmentInfo findShipmentOrThrow(Exchange exchange, UUID shipmentId) {
        ShipmentInfo shipment = shipmentInfoRepository.findById(shipmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Відправлення не знайдено: " + shipmentId));
        if (!shipment.getExchange().getId().equals(exchange.getId())) {
            throw new IllegalArgumentException("Відправлення належить іншому обміну");
        }
        return shipment;
    }

    private ExchangeResponse toResponse(Exchange exchange) {
        List<ExchangePhoto> photos = exchangePhotoRepository.findByExchangeId(exchange.getId());

        List<ExchangePhotoResponse> handoverPhotos = photos.stream()
                .filter(p -> p.getStage() == PhotoStage.HANDOVER)
                .map(this::toPhotoResponse)
                .toList();
        List<ExchangePhotoResponse> returnPhotos = photos.stream()
                .filter(p -> p.getStage() == PhotoStage.RETURN)
                .map(this::toPhotoResponse)
                .toList();
        List<ExchangePhotoResponse> disputePhotos = photos.stream()
                .filter(p -> p.getStage() == PhotoStage.DISPUTE)
                .map(this::toPhotoResponse)
                .toList();

        List<ShipmentInfoResponse> shipments = shipmentInfoRepository.findByExchangeId(exchange.getId()).stream()
                .map(s -> new ShipmentInfoResponse(
                        s.getId(), s.getDirection(), s.getRecipientName(), s.getRecipientPhone(),
                        s.getCarrier(), s.getCity(), s.getBranchNumber(), s.getWaybillPhotoUrl(),
                        s.getStatus(), s.getShippedAt(), s.getDeliveredAt()))
                .toList();

        List<DeadlineExtensionResponse> extensions = extensionRequestRepository
                .findByExchangeId(exchange.getId()).stream()
                .map(e -> new DeadlineExtensionResponse(
                        e.getId(), e.getExtraDays(), e.getRequestedNewDeadline(), e.getStatus(),
                        e.getComment(), e.getDecisionComment(), e.getCreatedAt(), e.getDecidedAt()))
                .toList();

        List<ExchangeReviewResponse> reviews = reviewRepository
                .findByExchangeId(exchange.getId()).stream()
                .map(r -> new ExchangeReviewResponse(
                        r.getId(),
                        r.getAuthor().getId(),
                        r.getAuthor().getName(),
                        r.getAuthor().getAvatarUrl(),
                        r.getTarget().getId(),
                        r.getRating(),
                        r.getComment(),
                        r.getCreatedAt()))
                .toList();

        var book = exchange.getListing().getBookCatalogEntry();

        return new ExchangeResponse(
                exchange.getId(),
                exchange.getListing().getId(),
                book.getTitle(),
                exchange.getOwner().getId(),
                exchange.getOwner().getName(),
                exchange.getReader().getId(),
                exchange.getReader().getName(),
                exchange.getDeliveryMethod(),
                exchange.getDeadline(),
                exchange.getExtendedDeadline(),
                exchange.getStatus(),
                handoverPhotos,
                returnPhotos,
                disputePhotos,
                exchange.getDisputeReason(),
                shipments,
                extensions,
                exchange.getCreatedAt(),
                exchange.getCompletedAt(),
                book.getAuthor(),
                book.getCoverUrl(),
                toParticipant(exchange.getOwner()),
                toParticipant(exchange.getReader()),
                reviews,
                exchange.getReadingDays(),
                exchange.getReadingStartedAt(),
                exchange.getReadingPausedDays(),
                exchange.getDueDate(),
                exchange.getReturnPreparedAt(),
                exchange.getReturnedAt(),
                isReadingPaused(exchange, shipments),
                disputeRepository.findByExchangeId(exchange.getId()).stream()
                        .max(Comparator.comparing(Dispute::getCreatedAt))
                        .map(ExchangeService::toDisputeResponse)
                        .orElse(null));
    }

    private static ExchangeDisputeResponse toDisputeResponse(Dispute dispute) {
        return new ExchangeDisputeResponse(
                dispute.getId(),
                dispute.getFiledBy().getId(),
                dispute.getDescription(),
                dispute.getStatus(),
                dispute.getResolutionComment(),
                dispute.getModerator() == null ? null : dispute.getModerator().getName(),
                dispute.getCreatedAt(),
                dispute.getResolvedAt());
    }

    private static boolean isReadingPaused(Exchange exchange, List<ShipmentInfoResponse> shipments) {
        return exchange.getDeliveryMethod() == DeliveryMethod.MAIL
                && exchange.getStatus() == ExchangeStatus.RETURN_PENDING
                && exchange.getReturnPreparedAt() != null
                && exchange.getReturnedAt() == null
                && shipments.stream().noneMatch(s -> s.direction() == ShipmentDirection.TO_OWNER);
    }

    private ExchangePhotoResponse toPhotoResponse(ExchangePhoto photo) {
        return new ExchangePhotoResponse(
                photo.getId(),
                photo.getUploadedBy().getId(),
                photo.getUploadedBy().getName(),
                photo.getStage(),
                photo.getUrl(),
                photo.getNote(),
                photo.getCreatedAt());
    }

    private ExchangeParticipantResponse toParticipant(User user) {
        // кількість обмінів — з реальних обмінів, а не зі збережених лічильників
        UserStatsService.UserExchangeStats stats = userStatsService.statsFor(user);
        return new ExchangeParticipantResponse(
                user.getId(),
                user.getName(),
                user.getAvatarUrl(),
                user.getRatingAvg(),
                stats.booksGiven(),
                stats.booksTaken(),
                stats.booksOverdue());
    }
}