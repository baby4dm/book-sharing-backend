package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.AddListingPhotoRequest;
import com.booksharing.dto.req.CreateListingRequest;
import com.booksharing.dto.req.UpdateListingRequest;
import com.booksharing.dto.res.ListingResponse;
import com.booksharing.entity.BookCatalogEntry;
import com.booksharing.entity.Listing;
import com.booksharing.entity.ListingPhoto;
import com.booksharing.entity.User;
import com.booksharing.enums.ListingStatus;
import com.booksharing.enums.SettlementType;
import com.booksharing.enums.UserRole;
import com.booksharing.mapper.ListingMapper;
import com.booksharing.repository.BookCatalogEntryRepository;
import com.booksharing.repository.ListingPhotoRepository;
import com.booksharing.repository.ListingRepository;
import com.booksharing.repository.UserRepository;
import com.booksharing.support.TestData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Оголошення про книги")
class ListingServiceTest {

    @Mock private ListingRepository listingRepository;
    @Mock private ListingPhotoRepository listingPhotoRepository;
    @Mock private BookCatalogEntryRepository bookCatalogEntryRepository;
    @Mock private UserRepository userRepository;
    @Mock private AccountGuard accountGuard;
    @Spy private ListingMapper listingMapper = Mappers.getMapper(ListingMapper.class);

    @InjectMocks
    private ListingService listingService;

    private User owner;
    private Listing listing;

    @BeforeEach
    void setUp() {
        owner = TestData.user("Власник");
        owner.setSettlementName("Рівне");
        owner.setRegion("Рівненська");
        owner.setSettlementType(SettlementType.CITY);
        listing = TestData.listing(owner);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(listingRepository.save(any(Listing.class))).thenAnswer(inv -> {
            Listing l = inv.getArgument(0);
            if (l.getId() == null) {
                l.setId(UUID.randomUUID());
            }
            return l;
        });
        when(listingPhotoRepository.findByListingId(listing.getId())).thenReturn(List.of(
                ListingPhoto.builder().url("/uploads/1.jpg").build()));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
    }

    @Test
    @DisplayName("створення оголошення з фото й місцем власника")
    void create() {
        BookCatalogEntry book = TestData.book("Кобзар");
        when(bookCatalogEntryRepository.findById(book.getId())).thenReturn(Optional.of(book));

        ListingResponse response = listingService.create(owner.getId(), new CreateListingRequest(
                book.getId(), "Як нова", List.of("PICKUP"), List.of("/uploads/a.jpg", "/uploads/b.jpg"), null, null, null));

        assertThat(response.bookTitle()).isEqualTo("Кобзар");
        assertThat(response.status()).isEqualTo(ListingStatus.AVAILABLE);
        assertThat(response.photoUrls()).hasSize(2);
        assertThat(response.settlementName()).isEqualTo("Рівне");
        assertThat(response.region()).isEqualTo("Рівненська");
        verify(listingPhotoRepository, times(2)).save(any(ListingPhoto.class));
        verify(accountGuard).requireCanCreateContent(owner.getId());
    }

    @Test
    @DisplayName("оголошення в іншому місті показує місто оголошення")
    void createInOtherCity() {
        BookCatalogEntry book = TestData.book("Кобзар");
        when(bookCatalogEntryRepository.findById(book.getId())).thenReturn(Optional.of(book));

        ListingResponse response = listingService.create(owner.getId(), new CreateListingRequest(
                book.getId(), "Як нова", List.of("MAIL"), null, SettlementType.VILLAGE, "Львівська", "Шкло"));

        assertThat(response.settlementName()).isEqualTo("Шкло");
        assertThat(response.settlementType()).isEqualTo(SettlementType.VILLAGE);
        assertThat(response.photoUrls()).isEmpty();
    }

    @Test
    @DisplayName("помилки створення: користувач, книга, способи доставки")
    void createErrors() {
        BookCatalogEntry book = TestData.book("Кобзар");
        when(bookCatalogEntryRepository.findById(book.getId())).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> listingService.create(UUID.randomUUID(), new CreateListingRequest(
                book.getId(), "x", List.of("PICKUP"), null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> listingService.create(owner.getId(), new CreateListingRequest(
                UUID.randomUUID(), "x", List.of("PICKUP"), null, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> listingService.create(owner.getId(), new CreateListingRequest(
                book.getId(), "x", List.of(), null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("щонайменше один");
        assertThatThrownBy(() -> listingService.create(owner.getId(), new CreateListingRequest(
                book.getId(), "x", null, null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listingService.create(owner.getId(), new CreateListingRequest(
                book.getId(), "x", List.of("TELEPORT"), null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("TELEPORT");
    }

    @Test
    @DisplayName("відкрите оголошення бачать усі, разом з гостями")
    void getVisible() {
        ListingResponse response = listingService.getById(listing.getId(), null);
        assertThat(response.id()).isEqualTo(listing.getId());
        assertThat(response.photoUrls()).containsExactly("/uploads/1.jpg");
        assertThat(response.ownerName()).isEqualTo("Власник");
    }

    @Test
    @DisplayName("приховане оголошення бачать лише власник і модератор")
    void getHidden() {
        listing.setHiddenAt(LocalDateTime.now());
        listing.setHiddenReason("Спам");

        assertThat(listingService.getById(listing.getId(), owner).hiddenReason()).isEqualTo("Спам");
        assertThat(listingService.getById(listing.getId(), TestData.user("Модератор", UserRole.MODERATOR))).isNotNull();
        assertThat(listingService.getById(listing.getId(), TestData.user("Адмін", UserRole.ADMIN))).isNotNull();
        assertThatThrownBy(() -> listingService.getById(listing.getId(), TestData.user("Інший")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> listingService.getById(listing.getId(), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("пошук без фільтра доставки використовує пагінацію бази")
    void searchPaged() {
        when(listingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(listing)));

        Page<ListingResponse> page = listingService.search(List.of("Поезія"), List.of("Рівне"), null,
                List.of(ListingStatus.AVAILABLE), "пісня", UUID.randomUUID(), PageRequest.of(0, 12));

        assertThat(page.getContent()).hasSize(1);
        verify(listingRepository, never()).findAll(any(Specification.class), any(Sort.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("фільтр доставки відбирає й ділить результати на сторінки")
    void searchByDelivery() {
        Listing mailOnly = TestData.listing(owner);
        mailOnly.setDeliveryMethods(List.of("MAIL"));
        Listing pickupOnly = TestData.listing(owner);
        pickupOnly.setDeliveryMethods(List.of("PICKUP"));
        when(listingRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(listing, mailOnly, pickupOnly));

        Page<ListingResponse> first = listingService.search(null, null, List.of("MAIL"), null, null, null, PageRequest.of(0, 1));
        Page<ListingResponse> beyond = listingService.search(null, null, List.of("MAIL"), null, null, null, PageRequest.of(5, 1));

        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.getContent()).hasSize(1);
        assertThat(beyond.getContent()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("мої оголошення й оголошення власника")
    void byOwner() {
        when(listingRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(listing)));
        assertThat(listingService.getMyListings(owner.getId(), null, null, List.of(), null, null, PageRequest.of(0, 12))
                .getContent()).hasSize(1);
        assertThat(listingService.getByOwner(owner.getId(), null, null, null, null, null, false, PageRequest.of(0, 8))
                .getContent()).hasSize(1);
    }

    @Test
    @DisplayName("оновлення полів оголошення власником")
    void update() {
        ListingResponse response = listingService.update(listing.getId(), owner.getId(), new UpdateListingRequest(
                "Є підписи на полях", List.of("MAIL"), ListingStatus.ARCHIVED, SettlementType.CITY, "Київська", "Київ"));

        assertThat(response.conditionDescription()).isEqualTo("Є підписи на полях");
        assertThat(response.deliveryMethods()).containsExactly("MAIL");
        assertThat(response.status()).isEqualTo(ListingStatus.ARCHIVED);
        assertThat(response.settlementName()).isEqualTo("Київ");
    }

    @Test
    @DisplayName("порожнє оновлення нічого не змінює")
    void emptyUpdate() {
        listingService.update(listing.getId(), owner.getId(), new UpdateListingRequest(null, null, null, null, null, null));
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.AVAILABLE);
        assertThat(listing.getConditionDescription()).isEqualTo("Гарний стан");
    }

    @Test
    @DisplayName("системні статуси вручну не встановлюються")
    void systemStatuses() {
        assertThatThrownBy(() -> listingService.update(listing.getId(), owner.getId(),
                new UpdateListingRequest(null, null, ListingStatus.RESERVED, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> listingService.update(listing.getId(), owner.getId(),
                new UpdateListingRequest(null, null, ListingStatus.IN_EXCHANGE, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("чуже оголошення не можна змінити, видалити чи доповнити фото")
    void notOwner() {
        UUID stranger = UUID.randomUUID();
        assertThatThrownBy(() -> listingService.update(listing.getId(), stranger,
                new UpdateListingRequest(null, null, null, null, null, null)))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> listingService.delete(listing.getId(), stranger))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> listingService.addPhoto(listing.getId(), stranger, new AddListingPhotoRequest("/x.jpg")))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> listingService.delete(UUID.randomUUID(), owner.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("видалення й додавання фото власником")
    void deleteAndAddPhoto() {
        listingService.addPhoto(listing.getId(), owner.getId(), new AddListingPhotoRequest("/uploads/2.jpg"));
        verify(listingPhotoRepository).save(any(ListingPhoto.class));
        listingService.delete(listing.getId(), owner.getId());
        verify(listingRepository).delete(listing);
    }

    @Test
    @DisplayName("список міст з оголошеннями")
    void cities() {
        when(listingRepository.findDistinctSettlementNames()).thenReturn(List.of("Київ", "Рівне"));
        assertThat(listingService.getAvailableCities()).containsExactly("Київ", "Рівне");
    }
}
