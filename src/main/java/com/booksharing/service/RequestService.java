package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.CreateRequestRequest;
import com.booksharing.dto.req.RejectRequestRequest;
import com.booksharing.dto.res.RequestListResponse;
import com.booksharing.dto.res.RequestResponse;
import com.booksharing.entity.ChatRoom;
import com.booksharing.entity.Exchange;
import com.booksharing.entity.Listing;
import com.booksharing.entity.Request;
import com.booksharing.entity.User;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.ListingStatus;
import com.booksharing.enums.NotificationType;
import com.booksharing.enums.RequestSort;
import com.booksharing.enums.RequestStatus;
import com.booksharing.mapper.RequestMapper;
import com.booksharing.repository.ChatRoomRepository;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.ListingRepository;
import com.booksharing.repository.RequestRepository;
import com.booksharing.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.booksharing.spec.RequestSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final AccountGuard accountGuard;

    @Transactional
    public RequestResponse submit(UUID listingId, UUID requesterId, CreateRequestRequest request) {
        accountGuard.requireCanCreateContent(requesterId);
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Оголошення не знайдено: " + listingId));
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено: " + requesterId));

        if (listing.getStatus() == ListingStatus.ARCHIVED) {
            throw new IllegalStateException("Оголошення знято з публікації, заявки більше не приймаються");
        }
        if (listing.isHidden()) {
            throw new IllegalStateException("Оголошення приховано модератором, заявки не приймаються");
        }
        if (listing.getOwner().getId().equals(requesterId)) {
            throw new IllegalArgumentException("Не можна подати заявку на власну книгу");
        }

        DeliveryMethod deliveryMethod = validateDeliveryMethod(listing, request.preferredDeliveryMethod());

        boolean bookIsBusy = listing.getStatus() == ListingStatus.RESERVED
                || listing.getStatus() == ListingStatus.IN_EXCHANGE;
        boolean alreadyInQueue = requestRepository.findByListingIdOrderByCreatedAtAsc(listingId).stream()
                .anyMatch(r -> r.getRequester().getId().equals(requesterId)
                        && (r.getStatus() == RequestStatus.PENDING
                        || (r.getStatus() == RequestStatus.ACTIVE && bookIsBusy)));
        if (alreadyInQueue) {
            throw new IllegalStateException("У вас уже є активна заявка на цю книгу");
        }

        Request entity = Request.builder()
                .listing(listing)
                .requester(requester)
                .readingDays(request.readingDays())
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

    @Transactional(readOnly = true)
    public List<RequestResponse> getQueueForListing(UUID listingId) {
        return toResponses(requestRepository.findByListingIdOrderByCreatedAtAsc(listingId));
    }

    @Transactional(readOnly = true)
    public RequestListResponse getMyRequests(UUID requesterId, String search,
                                             DeliveryMethod deliveryMethod, RequestStatus status,
                                             RequestSort sort, int page, int size) {
        return search(RequestSpecifications.sentBy(requesterId), true,
                search, deliveryMethod, status, sort, page, size);
    }

    @Transactional(readOnly = true)
    public RequestListResponse getReceivedRequests(UUID ownerId, String search,
                                                   DeliveryMethod deliveryMethod, RequestStatus status,
                                                   RequestSort sort, int page, int size) {
        return search(RequestSpecifications.receivedBy(ownerId), false,
                search, deliveryMethod, status, sort, page, size);
    }

    private RequestListResponse search(Specification<Request> scope, boolean personIsOwner,
                                       String search, DeliveryMethod deliveryMethod,
                                       RequestStatus status, RequestSort sort,
                                       int page, int size) {
        Specification<Request> base = Specification.allOf(
                scope,
                RequestSpecifications.matchesSearch(search, personIsOwner),
                RequestSpecifications.hasDeliveryMethod(deliveryMethod));

        Map<RequestStatus, Long> counts = new EnumMap<>(RequestStatus.class);
        for (RequestStatus s : RequestStatus.values()) {
            counts.put(s, requestRepository.count(
                    Specification.allOf(base, RequestSpecifications.hasStatus(s))));
        }
        long total = counts.values().stream().mapToLong(Long::longValue).sum();

        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                (sort == null ? RequestSort.NEWEST : sort).toSort());

        Page<Request> result = requestRepository.findAll(
                Specification.allOf(base, RequestSpecifications.hasStatus(status)),
                pageable);

        return new RequestListResponse(
                toResponses(result.getContent()),
                counts,
                total,
                result.getNumber(),
                result.getTotalPages(),
                result.getTotalElements());
    }

    @Transactional
    public RequestResponse approve(UUID requestId, UUID currentUserId) {
        Request request = findRequestOrThrow(requestId);
        requireListingOwnership(request, currentUserId);

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Заявку вже розглянуто");
        }

        Listing listing = request.getListing();
        boolean bookInExchange = exchangeRepository.existsByListingIdAndStatusNot(
                listing.getId(), ExchangeStatus.COMPLETED);
        if (bookInExchange) {
            throw new IllegalStateException("Книга зараз в іншому обміні. Схвалити нову заявку можна після його завершення");
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
                .readingDays(request.getReadingDays())
                .status(ExchangeStatus.HANDOVER_PENDING)
                .build();
        exchange = exchangeRepository.save(exchange);

        if (chatRoomRepository.findBetween(
                listing.getOwner().getId(), request.getRequester().getId()).isEmpty()) {
            chatRoomRepository.save(ChatRoom.builder()
                    .userA(listing.getOwner())
                    .userB(request.getRequester())
                    .build());
        }

        notificationService.notify(
                request.getRequester(),
                NotificationType.REQUEST_APPROVED,
                request.getId(),
                "Вашу заявку на книгу \"" + listing.getBookCatalogEntry().getTitle() + "\" підтверджено");

        return requestMapper.toResponse(request).withExchangeId(exchange.getId());
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
            throw new IllegalStateException("Скасувати можна лише заявку, яку ще не розглянули");
        }
        request.setStatus(RequestStatus.CANCELLED);
        request.setDecidedAt(LocalDateTime.now());
        requestRepository.save(request);
    }

    private List<RequestResponse> toResponses(List<Request> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        List<UUID> requestIds = requests.stream().map(Request::getId).toList();
        Map<UUID, UUID> exchangeByRequest = exchangeRepository.findByRequestIdIn(requestIds).stream()
                .collect(Collectors.toMap(e -> e.getRequest().getId(), Exchange::getId));

        return requests.stream()
                .map(r -> requestMapper.toResponse(r).withExchangeId(exchangeByRequest.get(r.getId())))
                .toList();
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