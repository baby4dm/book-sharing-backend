package com.booksharing.model.listing;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.model.bookcatalog.BookCatalogEntry;
import com.booksharing.model.bookcatalog.BookCatalogEntryRepository;
import com.booksharing.model.user.User;
import com.booksharing.model.user.UserRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

    public ListingResponse getById(UUID id) {
        Listing listing = findListingOrThrow(id);
        return listingMapper.toResponse(listing, photoUrlsOf(id));
    }

    public List<ListingResponse> search(String genre, String city, String deliveryMethod,
                                        ListingStatus status, String searchText) {
        Specification<Listing> spec = Specification
                .where(ListingSpecifications.hasStatus(status))
                .and(ListingSpecifications.hasGenre(genre))
                .and(ListingSpecifications.hasOwnerCity(city))
                .and(ListingSpecifications.matchesSearch(searchText));

        List<Listing> listings = listingRepository.findAll(spec);

        // фільтр за способом доставки - у Java, не в SQL (див. коментар
        // у ListingSpecifications)
        if (deliveryMethod != null && !deliveryMethod.isBlank()) {
            listings = listings.stream()
                    .filter(l -> l.getDeliveryMethods().contains(deliveryMethod))
                    .toList();
        }

        return listings.stream()
                .map(l -> listingMapper.toResponse(l, photoUrlsOf(l.getId())))
                .toList();
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