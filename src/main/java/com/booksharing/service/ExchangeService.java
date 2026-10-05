package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.*;
import com.booksharing.dto.res.DeadlineExtensionResponse;
import com.booksharing.dto.res.ExchangePhotoResponse;
import com.booksharing.dto.res.ExchangeResponse;
import com.booksharing.dto.res.ShipmentInfoResponse;
import com.booksharing.entity.*;
import com.booksharing.entity.Listing;
import com.booksharing.enums.*;
import com.booksharing.repository.ListingRepository;
import com.booksharing.entity.User;
import com.booksharing.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.booksharing.repository.DeadlineExtensionRequestRepository;
import com.booksharing.repository.ExchangePhotoRepository;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.ShipmentInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Стейт-машина обміну: {@code HANDOVER_PENDING → IN_READING → RETURN_PENDING
 * → COMPLETED}, плюс паралельні під-флоу дедлайнів (продовження) і
 * доставки поштою (окремо для кожного напрямку - видача читачу і
 * повернення власнику).
 */
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final ExchangeRepository exchangeRepository;
    private final ExchangePhotoRepository exchangePhotoRepository;
    private final ShipmentInfoRepository shipmentInfoRepository;
    private final DeadlineExtensionRequestRepository extensionRequestRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

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

    /**
     * Крок 1. Власник надсилає від 1 до 4 фото стану книги перед передачею.
     * Надсилання одноразове: додати фото на цьому етапі вдруге не можна.
     */
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
        return toResponse(exchange);
    }

    /**
     * Крок 3. Читач одним запитом надсилає від 1 до 4 фото при отриманні й
     * підтверджує отримання. Для доставки поштою це ж позначає посилку
     * доставленою - окремого підтвердження доставки немає.
     */
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

        exchange.setStatus(ExchangeStatus.IN_READING);
        exchangeRepository.save(exchange);

        Listing listing = exchange.getListing();
        listing.setStatus(ListingStatus.IN_EXCHANGE);
        listingRepository.save(listing);

        return toResponse(exchange);
    }

    /**
     * Крок 4. Читач надсилає від 1 до 4 фото стану книги перед поверненням -
     * обмін переходить у {@code RETURN_PENDING}. Повторно надіслати не можна
     * (після переходу статус уже не {@code IN_READING}).
     */
    @Transactional
    public ExchangeResponse submitReturnPhotos(UUID exchangeId, UUID currentUserId, SubmitPhotosRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireReader(exchange, currentUserId);
        requireStatus(exchange, ExchangeStatus.IN_READING,
                "Фото повернення можна надіслати лише під час читання");

        savePhotos(exchange, exchange.getReader(), PhotoStage.RETURN, request.urls());

        exchange.setStatus(ExchangeStatus.RETURN_PENDING);
        exchangeRepository.save(exchange);

        return toResponse(exchange);
    }

    /**
     * Крок 6. Власник підтверджує фінальне повернення - закриває обмін і
     * оновлює статистику обох учасників (враховуючи продовжений дедлайн).
     * Потребує фото читача; для пошти - що читач відправив книгу назад
     * (це ж позначає зворотну посилку доставленою).
     */
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

        exchange.setStatus(ExchangeStatus.COMPLETED);
        exchange.setCompletedAt(LocalDateTime.now());
        exchangeRepository.save(exchange);

        Listing listing = exchange.getListing();
        listing.setStatus(ListingStatus.AVAILABLE);
        listingRepository.save(listing);

        User owner = exchange.getOwner();
        owner.setBooksGiven(owner.getBooksGiven() + 1);
        userRepository.save(owner);

        User reader = exchange.getReader();
        reader.setBooksTaken(reader.getBooksTaken() + 1);
        LocalDate effectiveDeadline = exchange.getExtendedDeadline() != null
                ? exchange.getExtendedDeadline()
                : exchange.getDeadline();
        if (!LocalDate.now().isAfter(effectiveDeadline)) {
            reader.setBooksReturnedOnTime(reader.getBooksReturnedOnTime() + 1);
        } else {
            reader.setBooksOverdue(reader.getBooksOverdue() + 1);
        }
        userRepository.save(reader);

        return toResponse(exchange);
    }

    /** Фото-доказ до відкриття спору - і власник, і читач можуть додавати. */
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

    /**
     * Власник відкриває спір замість підтвердження повернення - вимагає
     * хоча б одне фото-доказ ({@link #addDisputePhoto}), щоб не можна
     * було відкрити спір "голослівно", без жодного матеріалу для розгляду.
     */
    @Transactional
    public ExchangeResponse openDispute(UUID exchangeId, UUID currentUserId, OpenDisputeRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireOwner(exchange, currentUserId);

        if (exchange.getStatus() != ExchangeStatus.RETURN_PENDING) {
            throw new IllegalStateException("Спір можна відкрити лише на етапі очікування повернення");
        }

        boolean hasDisputePhoto = exchangePhotoRepository.findByExchangeId(exchangeId).stream()
                .anyMatch(p -> p.getStage() == PhotoStage.DISPUTE);
        if (!hasDisputePhoto) {
            throw new IllegalStateException("Додайте хоча б одне фото-доказ перед відкриттям спору");
        }

        exchange.setStatus(ExchangeStatus.DISPUTED);
        exchange.setDisputeReason(request.reason());
        exchangeRepository.save(exchange);

        return toResponse(exchange);
    }

    @Transactional
    public ExchangeResponse requestExtension(UUID exchangeId, UUID currentUserId, CreateExtensionRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireReader(exchange, currentUserId);

        if (exchange.getStatus() != ExchangeStatus.IN_READING) {
            throw new IllegalStateException("Продовження дедлайну можна запросити лише під час читання");
        }

        extensionRequestRepository.save(DeadlineExtensionRequest.builder()
                .exchange(exchange)
                .requestedNewDeadline(request.requestedNewDeadline())
                .comment(request.comment())
                .status(ExtensionStatus.PENDING)
                .build());

        notificationService.notify(
                exchange.getOwner(),
                NotificationType.EXTENSION_REQUESTED,
                exchange.getId(),
                "Читач запросив продовження дедлайну для \""
                        + exchange.getListing().getBookCatalogEntry().getTitle() + "\"");

        return toResponse(exchange);
    }

    @Transactional
    public ExchangeResponse decideExtension(
            UUID exchangeId, UUID extensionRequestId, UUID currentUserId, boolean approve) {
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
        extensionRequestRepository.save(extension);

        if (approve) {
            exchange.setExtendedDeadline(extension.getRequestedNewDeadline());
            exchangeRepository.save(exchange);
        }

        notificationService.notify(
                exchange.getReader(),
                NotificationType.EXTENSION_DECIDED,
                exchange.getId(),
                approve ? "Продовження дедлайну підтверджено" : "У продовженні дедлайну відмовлено");

        return toResponse(exchange);
    }

    /**
     * Подається отримувачем посилки (не відправником): для {@code TO_READER}
     * - читачем, для {@code TO_OWNER} (при поверненні) - власником.
     */
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

        return toResponse(exchange);
    }

    /** Відправник (протилежна сторона від отримувача цього напрямку) вантажить накладну. */
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

        return toResponse(exchange);
    }

    // ==========================================================
    // Внутрішні допоміжні методи
    // ==========================================================

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
                        e.getId(), e.getRequestedNewDeadline(), e.getStatus(),
                        e.getComment(), e.getCreatedAt(), e.getDecidedAt()))
                .toList();

        return new ExchangeResponse(
                exchange.getId(),
                exchange.getListing().getId(),
                exchange.getListing().getBookCatalogEntry().getTitle(),
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
                exchange.getCompletedAt());
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
}