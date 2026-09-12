package com.booksharing.service;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.AddListingPhotoRequest;
import com.booksharing.dto.req.CreateListingRequest;
import com.booksharing.dto.res.ListingResponse;
import com.booksharing.entity.BookCatalogEntry;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.entity.Listing;
import com.booksharing.entity.ListingPhoto;
import com.booksharing.mapper.ListingMapper;
import com.booksharing.spec.ListingSpecifications;
import com.booksharing.enums.ListingStatus;
import com.booksharing.dto.req.UpdateListingRequest;
import com.booksharing.repository.BookCatalogEntryRepository;
import com.booksharing.entity.User;
import com.booksharing.repository.UserRepository;
import java.util.List;
import java.util.UUID;

import com.booksharing.repository.ListingPhotoRepository;
import com.booksharing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final ListingRepository listingRepository;
    private final ListingPhotoRepository listingPhotoRepository;
    private final BookCatalogEntryRepository bookCatalogEntryRepository;
    private final UserRepository userRepository;
    private final ListingMapper listingMapper;

    @Transactional
    public ListingResponse create(UUID ownerId, CreateListingRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Користувача не знайдено: " + ownerId));
        BookCatalogEntry book = bookCatalogEntryRepository.findById(request.bookCatalogEntryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Запис каталогу не знайдено: " + request.bookCatalogEntryId()));

        validateDeliveryMethods(request.deliveryMethods());

        Listing listing = Listing.builder()
                .owner(owner)
                .bookCatalogEntry(book)
                .conditionDescription(request.conditionDescription())
                .deliveryMethods(request.deliveryMethods())
                .status(ListingStatus.AVAILABLE)
                .build();
        listing = listingRepository.save(listing);

        List<String> photoUrls = request.photoUrls() == null ? List.of() : request.photoUrls();
        for (String url : photoUrls) {
            listingPhotoRepository.save(ListingPhoto.builder().listing(listing).url(url).build());
        }

        return listingMapper.toResponse(listing, photoUrls);
    }

    @Transactional(readOnly = true)
    public ListingResponse getById(UUID id) {
        Listing listing = findListingOrThrow(id);
        return listingMapper.toResponse(listing, photoUrlsOf(id));
    }

    /**
     * Пагінація роздвоюється навмисно: коли {@code deliveryMethod} не
     * заданий - фільтри повністю на рівні БД через {@code Specification},
     * і LIMIT/OFFSET теж рахує сама БД (ефективний шлях). Коли
     * {@code deliveryMethod} заданий - його фільтр застосовується в Java
     * (text[] не фільтрується через Specification, див. коментар у
     * {@link ListingSpecifications}), тому LIMIT/OFFSET на рівні БД
     * "відрізав" би елементи ДО цього java-фільтра і зламав би підрахунок
     * сторінок - тому в цьому випадку пагінація теж рахується вручну,
     * вже після фільтра.
     */
    @Transactional(readOnly = true)
    public Page<ListingResponse> search(String genre, String city, String deliveryMethod,
                                        ListingStatus status, String searchText, Pageable pageable) {
        Specification<Listing> spec = Specification
                .where(ListingSpecifications.hasStatus(status))
                .and(ListingSpecifications.hasGenre(genre))
                .and(ListingSpecifications.hasOwnerCity(city))
                .and(ListingSpecifications.matchesSearch(searchText));

        if (deliveryMethod == null || deliveryMethod.isBlank()) {
            return listingRepository.findAll(spec, pageable)
                    .map(l -> listingMapper.toResponse(l, photoUrlsOf(l.getId())));
        }

        // сортування рахуємо на рівні БД (ORDER BY) навіть тут - фільтр
        // по deliveryMethod далі тільки ВИДАЛЯЄ елементи, не переставляє
        // їх, тому порядок, заданий сортуванням, лишається правильним
        List<Listing> filtered = listingRepository.findAll(spec, pageable.getSort()).stream()
                .filter(l -> l.getDeliveryMethods().contains(deliveryMethod))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), filtered.size());
        List<ListingResponse> pageContent = start >= filtered.size()
                ? List.of()
                : filtered.subList(start, end).stream()
                .map(l -> listingMapper.toResponse(l, photoUrlsOf(l.getId())))
                .toList();

        return new PageImpl<>(pageContent, pageable, filtered.size());
    }

    @Transactional
    public ListingResponse update(UUID id, UUID currentUserId, UpdateListingRequest request) {
        Listing listing = findListingOrThrow(id);
        requireOwnership(listing, currentUserId);

        if (request.status() != null) {
            if (request.status() == ListingStatus.RESERVED || request.status() == ListingStatus.IN_EXCHANGE) {
                throw new IllegalArgumentException(
                        "Цей статус встановлюється автоматично системою, а не вручну");
            }
            listing.setStatus(request.status());
        }
        if (request.conditionDescription() != null) {
            listing.setConditionDescription(request.conditionDescription());
        }
        if (request.deliveryMethods() != null) {
            validateDeliveryMethods(request.deliveryMethods());
            listing.setDeliveryMethods(request.deliveryMethods());
        }

        listing = listingRepository.save(listing);
        return listingMapper.toResponse(listing, photoUrlsOf(id));
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        Listing listing = findListingOrThrow(id);
        requireOwnership(listing, currentUserId);
        listingRepository.delete(listing);
    }

    @Transactional
    public ListingResponse addPhoto(UUID id, UUID currentUserId, AddListingPhotoRequest request) {
        Listing listing = findListingOrThrow(id);
        requireOwnership(listing, currentUserId);

        listingPhotoRepository.save(ListingPhoto.builder().listing(listing).url(request.url()).build());
        return listingMapper.toResponse(listing, photoUrlsOf(id));
    }

    private void requireOwnership(Listing listing, UUID currentUserId) {
        if (!listing.getOwner().getId().equals(currentUserId)) {
            throw new AccessDeniedException("Це оголошення належить іншому користувачу");
        }
    }

    private Listing findListingOrThrow(UUID id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Оголошення не знайдено: " + id));
    }

    private List<String> photoUrlsOf(UUID listingId) {
        return listingPhotoRepository.findByListingId(listingId).stream()
                .map(ListingPhoto::getUrl)
                .toList();
    }

    private void validateDeliveryMethods(List<String> methods) {
        if (methods == null || methods.isEmpty()) {
            throw new IllegalArgumentException("Потрібно вказати щонайменше один спосіб доставки");
        }
        for (String method : methods) {
            try {
                DeliveryMethod.valueOf(method);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Невідомий спосіб доставки: " + method);
            }
        }
    }
}