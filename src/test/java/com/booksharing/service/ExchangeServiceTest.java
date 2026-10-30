package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.dto.req.SubmitPhotosRequest;
import com.booksharing.entity.BookCatalogEntry;
import com.booksharing.entity.Exchange;
import com.booksharing.entity.ExchangePhoto;
import com.booksharing.entity.Listing;
import com.booksharing.entity.User;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.ListingStatus;
import com.booksharing.enums.PhotoStage;
import com.booksharing.repository.DeadlineExtensionRequestRepository;
import com.booksharing.repository.DisputeRepository;
import com.booksharing.repository.ExchangePhotoRepository;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.ListingRepository;
import com.booksharing.repository.ReviewRepository;
import com.booksharing.repository.ShipmentInfoRepository;
import com.booksharing.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@DisplayName("Підтвердження отримання книжки читачем")
class ExchangeServiceTest {

    @Mock private ExchangeRepository exchangeRepository;
    @Mock private ExchangePhotoRepository exchangePhotoRepository;
    @Mock private ShipmentInfoRepository shipmentInfoRepository;
    @Mock private DeadlineExtensionRequestRepository extensionRequestRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ExchangeNotifier exchangeNotifier;
    @Mock private DisputeRepository disputeRepository;
    @Mock private UserStatsService userStatsService;

    @InjectMocks
    private ExchangeService exchangeService;

    private User owner;
    private User reader;
    private Listing listing;
    private Exchange exchange;
    private final SubmitPhotosRequest photos = new SubmitPhotosRequest(List.of("/uploads/received.jpg"));

    @BeforeEach
    void setUp() {
        owner = User.builder().id(UUID.randomUUID()).name("Власник").build();
        reader = User.builder().id(UUID.randomUUID()).name("Читач").build();
        BookCatalogEntry book = BookCatalogEntry.builder().title("Місто").author("Валер'ян Підмогильний").build();
        listing = Listing.builder().id(UUID.randomUUID()).owner(owner).bookCatalogEntry(book).status(ListingStatus.RESERVED).build();
        exchange = Exchange.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .reader(reader)
                .listing(listing)
                .deliveryMethod(DeliveryMethod.PICKUP)
                .readingDays(14)
                .build();
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
    }

    private ExchangePhoto ownerHandoverPhoto() {
        return ExchangePhoto.builder().exchange(exchange).uploadedBy(owner).stage(PhotoStage.HANDOVER).url("/uploads/sent.jpg").build();
    }

    @Test
    @DisplayName("підтвердити отримання може лише читач")
    void onlyReaderCanConfirm() {
        assertThatThrownBy(() -> exchangeService.confirmReceived(exchange.getId(), owner.getId(), photos))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.HANDOVER_PENDING);
    }

    @Test
    @DisplayName("без фото власника читання не починається")
    void ownerPhotoIsRequired() {
        assertThatThrownBy(() -> exchangeService.confirmReceived(exchange.getId(), reader.getId(), photos))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("фото");
        verify(exchangeRepository, never()).save(any());
    }

    @Test
    @DisplayName("повторне підтвердження відхиляється")
    void cannotConfirmTwice() {
        exchange.setStatus(ExchangeStatus.IN_READING);
        assertThatThrownBy(() -> exchangeService.confirmReceived(exchange.getId(), reader.getId(), photos))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("після підтвердження починається читання з датою повернення")
    void startsReading() {
        when(exchangePhotoRepository.findByExchangeId(exchange.getId())).thenReturn(List.of(ownerHandoverPhoto()));
        when(userStatsService.statsFor(any())).thenReturn(new UserStatsService.UserExchangeStats(0, 0, 0, 0, 0));

        exchangeService.confirmReceived(exchange.getId(), reader.getId(), photos);

        assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.IN_READING);
        assertThat(exchange.getReadingStartedAt()).isNotNull();
        assertThat(exchange.getDeadline()).isEqualTo(LocalDate.now().plusDays(14));
        assertThat(exchange.getDueDate()).isEqualTo(LocalDate.now().plusDays(14));
        assertThat(listing.getStatus()).isEqualTo(ListingStatus.IN_EXCHANGE);
        verify(exchangePhotoRepository).save(any(ExchangePhoto.class));
        verify(exchangeNotifier).bookReceived(exchange);
    }
}
