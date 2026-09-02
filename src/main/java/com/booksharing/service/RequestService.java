package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.CreateRequestRequest;
import com.booksharing.dto.req.RejectRequestRequest;
import com.booksharing.dto.res.RequestResponse;
import com.booksharing.entity.*;
import com.booksharing.enums.*;
import com.booksharing.mapper.RequestMapper;
import com.booksharing.repository.ChatRoomRepository;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.ListingRepository;
import com.booksharing.repository.RequestRepository;
import com.booksharing.entity.User;
import com.booksharing.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Черга заявок навмисно не блокує подання нових заявок, поки одна вже
 * {@code ACTIVE} - усі учасники (не лише власник) бачать повну чергу
 * (див. {@link #getQueueForListing}), і чекають рішення власника по черзі.
 */
@Service
@RequiredArgsConstructor
public class RequestService {

    private final RequestRepository requestRepository;
    private final ListingRepository listingRepository;
    private final ExchangeRepository exchangeRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final RequestMapper requestMapper;

    @Transactional
    public RequestResponse submit(UUID listingId, UUID requesterId, CreateRequestRequest request) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Оголошення не знайдено: " + listingId));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено: " + requesterId));

        if (listing.getStatus() == ListingStatus.ARCHIVED) {
            throw new IllegalStateException("Оголошення знято з публікації, заявки більше не приймаються");
        }
        if (listing.getOwner().getId().equals(requesterId)) {
            throw new IllegalArgumentException("Не можна подати заявку на власну книгу");
        }

        DeliveryMethod deliveryMethod = validateDeliveryMethod(listing, request.preferredDeliveryMethod());

        boolean alreadyInQueue = requestRepository.findByListingIdOrderByCreatedAtAsc(listingId).stream()
                .anyMatch(r -> r.getRequester().getId().equals(requesterId)
                        && (r.getStatus() == RequestStatus.PENDING || r.getStatus() == RequestStatus.ACTIVE));
        if (alreadyInQueue) {
            throw new IllegalStateException("У вас уже є активна заявка на цю книгу");
        }

        Request entity = Request.builder()
                .listing(listing)
                .requester(requester)
                .desiredDeadline(request.desiredDeadline())
                .preferredDeliveryMethod(deliveryMethod)
                .message(request.message())
                .status(RequestStatus.PENDING)
                .build();

        entity = requestRepository.save(entity);

        notificationService.notify(
                listing.getOwner(),
                NotificationType.NEW_REQUEST,
                entity.getId(),
                "Нова заявка на вашу книгу \"" + listing.getBookCatalogEntry().getTitle() + "\"");

        return requestMapper.toResponse(entity);
    }

    /** Видно всім, не лише власнику - учасники черги бачать повну картину. */
    public List<RequestResponse> getQueueForListing(UUID listingId) {
        return requestRepository.findByListingIdOrderByCreatedAtAsc(listingId).stream()
                .map(requestMapper::toResponse)
                .toList();
    }

    public List<RequestResponse> getMyRequests(UUID requesterId) {
        return requestRepository.findByRequesterId(requesterId).stream()
                .map(requestMapper::toResponse)
                .toList();
    }

    @Transactional
    public RequestResponse approve(UUID requestId, UUID currentUserId) {
        Request request = findRequestOrThrow(requestId);
        requireListingOwnership(request, currentUserId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Заявку вже розглянуто");
        }

        Listing listing = request.getListing();
        boolean hasActive = requestRepository.findByListingIdAndStatus(listing.getId(), RequestStatus.ACTIVE)
                .isPresent();
        if (hasActive) {
            throw new IllegalStateException("На це оголошення вже є активна заявка");
        }

        request.setStatus(RequestStatus.ACTIVE);
        request.setDecidedAt(LocalDateTime.now());
        request = requestRepository.save(request);

        listing.setStatus(ListingStatus.RESERVED);
        listingRepository.save(listing);

        Exchange exchange = Exchange.builder()
                .request(request)
                .listing(listing)
                .owner(listing.getOwner())
                .reader(request.getRequester())
                .deliveryMethod(request.getPreferredDeliveryMethod())
                .deadline(request.getDesiredDeadline())
                .status(ExchangeStatus.HANDOVER_PENDING)
                .build();
        exchangeRepository.save(exchange);

        chatRoomRepository.save(ChatRoom.builder()
                .exchange(exchange)
                .userA(listing.getOwner())
                .userB(request.getRequester())
                .build());

        notificationService.notify(
                request.getRequester(),
                NotificationType.REQUEST_APPROVED,
                request.getId(),
                "Вашу заявку на книгу \"" + listing.getBookCatalogEntry().getTitle() + "\" підтверджено");

        return requestMapper.toResponse(request);
    }

    @Transactional
    public RequestResponse reject(UUID requestId, UUID currentUserId, RejectRequestRequest request) {
        Request entity = findRequestOrThrow(requestId);
        requireListingOwnership(entity, currentUserId);

        if (entity.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Заявку вже розглянуто");
        }

        entity.setStatus(RequestStatus.REJECTED);
        entity.setRejectComment(request.comment());
        entity.setDecidedAt(LocalDateTime.now());
        entity = requestRepository.save(entity);

        notificationService.notify(
                entity.getRequester(),
                NotificationType.REQUEST_REJECTED,
                entity.getId(),
                "Вашу заявку на книгу \"" + entity.getListing().getBookCatalogEntry().getTitle()
                        + "\" відхилено: " + request.comment());

        return requestMapper.toResponse(entity);
    }

    @Transactional
    public void cancel(UUID requestId, UUID currentUserId) {
        Request request = findRequestOrThrow(requestId);
        if (!request.getRequester().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Це не ваша заявка");
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            // скасування вже ACTIVE заявки означало б ще й відкат Listing/Exchange -
            // цей сценарій буде окремим ендпоінтом у модулі Exchange
            throw new IllegalStateException("Скасувати можна лише заявку, яку ще не розглянули");
        }
        request.setStatus(RequestStatus.CANCELLED);
        request.setDecidedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    private void requireListingOwnership(Request request, UUID currentUserId) {
        if (!request.getListing().getOwner().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Це не ваше оголошення");
        }
    }

    private Request findRequestOrThrow(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Заявку не знайдено: " + requestId));
    }

    private DeliveryMethod validateDeliveryMethod(Listing listing, String requested) {
        DeliveryMethod method;
        try {
            method = DeliveryMethod.valueOf(requested);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Невідомий спосіб доставки: " + requested);
        }
        boolean supported = listing.getDeliveryMethods().contains(method.name());
        if (!supported) {
            throw new IllegalArgumentException("Це оголошення не підтримує спосіб доставки: " + requested);
        }
        return method;
    }
}