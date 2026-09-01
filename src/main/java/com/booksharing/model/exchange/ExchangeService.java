package com.booksharing.model.exchange;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.model.listing.Listing;
import com.booksharing.model.listing.ListingRepository;
import com.booksharing.model.listing.ListingStatus;
import com.booksharing.model.user.User;
import com.booksharing.model.user.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
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

    public ExchangeResponse getById(UUID id) {
        return toResponse(findExchangeOrThrow(id));
    }

    public List<ExchangeResponse> getMyExchanges(UUID userId) {
        List<Exchange> asOwner = exchangeRepository.findByOwnerId(userId);
        List<Exchange> asReader = exchangeRepository.findByReaderId(userId);
        return java.util.stream.Stream.concat(asOwner.stream(), asReader.stream())
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ExchangeResponse addHandoverPhoto(UUID exchangeId, UUID currentUserId, AddExchangePhotoRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        User uploader = requireParticipant(exchange, currentUserId);

        if (exchange.getStatus() != ExchangeStatus.HANDOVER_PENDING) {
            throw new IllegalStateException("Фото передачі можна додавати лише до підтвердження отримання");
        }

        exchangePhotoRepository.save(ExchangePhoto.builder()
                .exchange(exchange)
                .uploadedBy(uploader)
                .stage(PhotoStage.HANDOVER)
                .url(request.url())
                .note(request.note())
                .build());

        return toResponse(exchange);
    }

    /** Читач підтверджує фактичне отримання книги (незалежно від способу доставки). */
    @Transactional
    public ExchangeResponse confirmReceived(UUID exchangeId, UUID currentUserId) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireReader(exchange, currentUserId);

        if (exchange.getStatus() != ExchangeStatus.HANDOVER_PENDING) {
            throw new IllegalStateException("Обмін не очікує підтвердження отримання");
        }

        exchange.setStatus(ExchangeStatus.IN_READING);
        exchangeRepository.save(exchange);

        Listing listing = exchange.getListing();
        listing.setStatus(ListingStatus.IN_EXCHANGE);
        listingRepository.save(listing);

        return toResponse(exchange);
    }

    /**
     * Перший виклик із {@code IN_READING} автоматично переводить обмін у
     * {@code RETURN_PENDING} - окремого ендпоінта "почати повернення" не
     * потрібно, читач просто починає документувати стан книги.
     */
    @Transactional
    public ExchangeResponse addReturnPhoto(UUID exchangeId, UUID currentUserId, AddExchangePhotoRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        User uploader = requireParticipant(exchange, currentUserId);

        if (exchange.getStatus() != ExchangeStatus.IN_READING
                && exchange.getStatus() != ExchangeStatus.RETURN_PENDING) {
            throw new IllegalStateException("Фото повернення можна додавати лише під час читання або повернення");
        }

        if (exchange.getStatus() == ExchangeStatus.IN_READING) {
            exchange.setStatus(ExchangeStatus.RETURN_PENDING);
            exchangeRepository.save(exchange);
        }

        exchangePhotoRepository.save(ExchangePhoto.builder()
                .exchange(exchange)
                .uploadedBy(uploader)
                .stage(PhotoStage.RETURN)
                .url(request.url())
                .note(request.note())
                .build());

        return toResponse(exchange);
    }

    /**
     * Власник підтверджує фінальне повернення - закриває обмін і оновлює
     * статистику обох учасників (враховуючи продовжений дедлайн, якщо він був).
     */
    @Transactional
    public ExchangeResponse confirmReturn(UUID exchangeId, UUID currentUserId) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        requireOwner(exchange, currentUserId);

        if (exchange.getStatus() != ExchangeStatus.RETURN_PENDING) {
            throw new IllegalStateException("Обмін не очікує підтвердження повернення");
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

        return toResponse(exchange);
    }

    /**
     * Подається отримувачем посилки (не відправником): для {@code TO_READER}
     * - читачем, для {@code TO_OWNER} (при поверненні) - власником.
     */
    @Transactional
    public ExchangeResponse createShipment(UUID exchangeId, UUID currentUserId, CreateShipmentRequest request) {
        Exchange exchange = findExchangeOrThrow(exchangeId);

        if (exchange.getDeliveryMethod() != com.booksharing.model.listing.DeliveryMethod.MAIL) {
            throw new IllegalStateException("Цей обмін не передбачає доставку поштою");
        }

        UUID expectedRecipientId = request.direction() == ShipmentDirection.TO_READER
                ? exchange.getReader().getId()
                : exchange.getOwner().getId();
        if (!expectedRecipientId.equals(currentUserId)) {
            throw new AccessDeniedException("Контактні дані для цього напрямку вказує інший учасник обміну");
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
        if (shipment.getStatus() != ShipmentStatus.PENDING) {
            throw new IllegalStateException("Це відправлення вже позначено як відправлене");
        }

        shipment.setWaybillPhotoUrl(request.waybillPhotoUrl());
        shipment.setStatus(ShipmentStatus.SHIPPED);
        shipment.setShippedAt(LocalDateTime.now());
        shipmentInfoRepository.save(shipment);

        return toResponse(exchange);
    }

    /** Отримувач (той самий, хто вказував контактні дані) підтверджує доставку. */
    @Transactional
    public ExchangeResponse confirmDelivered(UUID exchangeId, UUID shipmentId, UUID currentUserId) {
        Exchange exchange = findExchangeOrThrow(exchangeId);
        ShipmentInfo shipment = findShipmentOrThrow(exchange, shipmentId);

        UUID expectedRecipientId = shipment.getDirection() == ShipmentDirection.TO_READER
                ? exchange.getReader().getId()
                : exchange.getOwner().getId();
        if (!expectedRecipientId.equals(currentUserId)) {
            throw new AccessDeniedException("Підтвердити доставку може лише отримувач цього відправлення");
        }
        if (shipment.getStatus() != ShipmentStatus.SHIPPED) {
            throw new IllegalStateException("Це відправлення ще не позначено як відправлене");
        }

        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(LocalDateTime.now());
        shipmentInfoRepository.save(shipment);

        return toResponse(exchange);
    }

    // ==========================================================
    // Внутрішні допоміжні методи
    // ==========================================================

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