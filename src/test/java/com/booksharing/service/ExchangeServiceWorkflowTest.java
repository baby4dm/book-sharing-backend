package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.AddExchangePhotoRequest;
import com.booksharing.dto.req.CreateExtensionRequest;
import com.booksharing.dto.req.CreateShipmentRequest;
import com.booksharing.dto.req.OpenDisputeRequest;
import com.booksharing.dto.req.ShipWaybillRequest;
import com.booksharing.dto.req.SubmitPhotosRequest;
import com.booksharing.dto.res.ExchangeListResponse;
import com.booksharing.dto.res.ExchangeResponse;
import com.booksharing.entity.DeadlineExtensionRequest;
import com.booksharing.entity.Dispute;
import com.booksharing.entity.Exchange;
import com.booksharing.entity.ExchangePhoto;
import com.booksharing.entity.Review;
import com.booksharing.entity.ShipmentInfo;
import com.booksharing.entity.User;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.DisputeStatus;
import com.booksharing.enums.ExchangeRole;
import com.booksharing.enums.ExchangeSort;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.ExtensionStatus;
import com.booksharing.enums.ListingStatus;
import com.booksharing.enums.NotificationType;
import com.booksharing.enums.PhotoStage;
import com.booksharing.enums.ShipmentCarrier;
import com.booksharing.enums.ShipmentDirection;
import com.booksharing.enums.ShipmentStatus;
import com.booksharing.repository.DeadlineExtensionRequestRepository;
import com.booksharing.repository.DisputeRepository;
import com.booksharing.repository.ExchangePhotoRepository;
import com.booksharing.repository.ExchangeRepository;
import com.booksharing.repository.ListingRepository;
import com.booksharing.repository.ReviewRepository;
import com.booksharing.repository.ShipmentInfoRepository;
import com.booksharing.repository.UserRepository;
import com.booksharing.support.TestData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Етапи обміну книгою")
class ExchangeServiceWorkflowTest {

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
    private Exchange exchange;
    private final List<ExchangePhoto> photos = new ArrayList<>();
    private final SubmitPhotosRequest twoPhotos = new SubmitPhotosRequest(List.of("/uploads/a.jpg", "/uploads/b.jpg"));

    @BeforeEach
    void setUp() {
        owner = TestData.user("Власник");
        reader = TestData.user("Читач");
        exchange = TestData.exchange(owner, reader, DeliveryMethod.PICKUP, ExchangeStatus.HANDOVER_PENDING);
        when(exchangeRepository.findById(exchange.getId())).thenReturn(Optional.of(exchange));
        when(exchangePhotoRepository.findByExchangeId(exchange.getId())).thenReturn(photos);
        when(userStatsService.statsFor(any())).thenReturn(TestData.noStats());
    }

    private ExchangePhoto photo(User by, PhotoStage stage) {
        ExchangePhoto p = ExchangePhoto.builder()
                .id(UUID.randomUUID()).exchange(exchange).uploadedBy(by).stage(stage).url("/uploads/x.jpg").build();
        photos.add(p);
        return p;
    }

    private ShipmentInfo shipment(ShipmentDirection direction, ShipmentStatus status) {
        ShipmentInfo s = ShipmentInfo.builder()
                .id(UUID.randomUUID()).exchange(exchange).direction(direction).status(status)
                .carrier(ShipmentCarrier.NOVA_POSHTA).city("Рівне").branchNumber("5").build();
        when(shipmentInfoRepository.findByExchangeIdAndDirection(exchange.getId(), direction)).thenReturn(Optional.of(s));
        when(shipmentInfoRepository.findById(s.getId())).thenReturn(Optional.of(s));
        return s;
    }

    @Nested
    @DisplayName("перегляд")
    class Read {

        @Test
        @DisplayName("відповідь містить фото за етапами, відправлення, продовження, відгуки й спір")
        void fullResponse() {
            photo(owner, PhotoStage.HANDOVER);
            photo(reader, PhotoStage.RETURN);
            photo(owner, PhotoStage.DISPUTE);
            ShipmentInfo s = ShipmentInfo.builder().id(UUID.randomUUID()).direction(ShipmentDirection.TO_READER)
                    .status(ShipmentStatus.SHIPPED).build();
            when(shipmentInfoRepository.findByExchangeId(exchange.getId())).thenReturn(List.of(s));
            when(extensionRequestRepository.findByExchangeId(exchange.getId())).thenReturn(List.of(
                    DeadlineExtensionRequest.builder().id(UUID.randomUUID()).extraDays(5).build()));
            when(reviewRepository.findByExchangeId(exchange.getId())).thenReturn(List.of(Review.builder()
                    .id(UUID.randomUUID()).author(reader).target(owner).rating((short) 5).comment("Дякую").build()));
            Dispute older = Dispute.builder().id(UUID.randomUUID()).filedBy(owner).status(DisputeStatus.DISMISSED)
                    .createdAt(LocalDateTime.of(2026, 9, 1, 10, 0)).build();
            Dispute newer = Dispute.builder().id(UUID.randomUUID()).filedBy(reader).status(DisputeStatus.OPEN)
                    .moderator(TestData.user("Модератор")).createdAt(LocalDateTime.of(2026, 9, 5, 10, 0)).build();
            when(disputeRepository.findByExchangeId(exchange.getId())).thenReturn(List.of(older, newer));

            ExchangeResponse response = exchangeService.getById(exchange.getId());

            assertThat(response.handoverPhotos()).hasSize(1);
            assertThat(response.returnPhotos()).hasSize(1);
            assertThat(response.disputePhotos()).hasSize(1);
            assertThat(response.shipments()).hasSize(1);
            assertThat(response.extensionRequests()).hasSize(1);
            assertThat(response.reviews()).hasSize(1);
            assertThat(response.reviews().get(0).rating()).isEqualTo(5);
            assertThat(response.dispute().id()).isEqualTo(newer.getId());
            assertThat(response.dispute().moderatorName()).isEqualTo("Модератор");
            assertThat(response.bookTitle()).isEqualTo("Лісова пісня");
            assertThat(response.readingPaused()).isFalse();
        }

        @Test
        @DisplayName("неіснуючий обмін")
        void notFound() {
            assertThatThrownBy(() -> exchangeService.getById(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("мої обміни об'єднують обміни власника й читача")
        void myExchanges() {
            Exchange other = TestData.exchange(TestData.user("Хтось"), owner, DeliveryMethod.MAIL, ExchangeStatus.IN_READING);
            when(exchangeRepository.findByOwnerId(owner.getId())).thenReturn(List.of(exchange));
            when(exchangeRepository.findByReaderId(owner.getId())).thenReturn(List.of(other));
            assertThat(exchangeService.getMyExchanges(owner.getId())).hasSize(2);
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("пошук рахує обміни за статусами й обмежує розмір сторінки")
        void search() {
            when(exchangeRepository.count(any(Specification.class))).thenReturn(1L);
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            when(exchangeRepository.findAll(any(Specification.class), pageable.capture()))
                    .thenReturn(new PageImpl<>(List.of(exchange)));

            ExchangeListResponse result = exchangeService.searchMyExchanges(owner.getId(), "пісня",
                    ExchangeRole.OWNER, DeliveryMethod.PICKUP, ExchangeStatus.HANDOVER_PENDING, ExchangeSort.NEWEST, -1, 999);

            assertThat(result.items()).hasSize(1);
            assertThat(result.total()).isEqualTo(ExchangeStatus.values().length);
            assertThat(pageable.getValue().getPageNumber()).isZero();
            assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        }
    }

    @Nested
    @DisplayName("передача книги")
    class Handover {

        @Test
        @DisplayName("власник надсилає фото й читач отримує сповіщення")
        void ownerSendsPhotos() {
            exchangeService.submitHandoverPhotos(exchange.getId(), owner.getId(), twoPhotos);
            verify(exchangePhotoRepository, times(2)).save(any(ExchangePhoto.class));
            verify(exchangeNotifier).handoverReady(exchange);
        }

        @Test
        @DisplayName("повторно надіслати фото не можна")
        void onlyOnce() {
            photo(owner, PhotoStage.HANDOVER);
            assertThatThrownBy(() -> exchangeService.submitHandoverPhotos(exchange.getId(), owner.getId(), twoPhotos))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("вже надіслали");
        }

        @Test
        @DisplayName("читач не може надсилати фото передачі за власника")
        void readerCannot() {
            assertThatThrownBy(() -> exchangeService.submitHandoverPhotos(exchange.getId(), reader.getId(), twoPhotos))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("після початку читання фото передачі вже не приймаються")
        void wrongStatus() {
            exchange.setStatus(ExchangeStatus.IN_READING);
            assertThatThrownBy(() -> exchangeService.submitHandoverPhotos(exchange.getId(), owner.getId(), twoPhotos))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("поштою: отримання можливе лише після відправлення")
        void mailNeedsShipment() {
            exchange.setDeliveryMethod(DeliveryMethod.MAIL);
            photo(owner, PhotoStage.HANDOVER);
            assertThatThrownBy(() -> exchangeService.confirmReceived(exchange.getId(), reader.getId(), twoPhotos))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Власник ще не відправив книгу");
        }

        @Test
        @DisplayName("поштою: підтвердження отримання позначає відправлення доставленим")
        void mailMarksDelivered() {
            exchange.setDeliveryMethod(DeliveryMethod.MAIL);
            photo(owner, PhotoStage.HANDOVER);
            ShipmentInfo toReader = shipment(ShipmentDirection.TO_READER, ShipmentStatus.SHIPPED);

            ExchangeResponse response = exchangeService.confirmReceived(exchange.getId(), reader.getId(), twoPhotos);

            assertThat(toReader.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
            assertThat(toReader.getDeliveredAt()).isNotNull();
            assertThat(response.status()).isEqualTo(ExchangeStatus.IN_READING);
            assertThat(exchange.getListing().getStatus()).isEqualTo(ListingStatus.IN_EXCHANGE);
        }
    }

    @Nested
    @DisplayName("повернення книги")
    class Return {

        @BeforeEach
        void reading() {
            exchange.setStatus(ExchangeStatus.IN_READING);
            exchange.setDeadline(LocalDate.now().plusDays(3));
        }

        @Test
        @DisplayName("при особистій зустрічі повернення фіксується одразу")
        void pickupReturn() {
            exchangeService.submitReturnPhotos(exchange.getId(), reader.getId(), twoPhotos);
            assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.RETURN_PENDING);
            assertThat(exchange.getReturnPreparedAt()).isNotNull();
            assertThat(exchange.getReturnedAt()).isNotNull();
            verify(exchangeNotifier).returnStarted(exchange);
        }

        @Test
        @DisplayName("поштою: дата повернення ставиться лише після відправлення")
        void mailReturnWaitsForShipment() {
            exchange.setDeliveryMethod(DeliveryMethod.MAIL);
            ExchangeResponse response = exchangeService.submitReturnPhotos(exchange.getId(), reader.getId(), twoPhotos);
            assertThat(exchange.getReturnedAt()).isNull();
            assertThat(response.readingPaused()).isTrue();
        }

        @Test
        @DisplayName("прострочену книгу теж можна повернути")
        void overdueReturn() {
            exchange.setStatus(ExchangeStatus.OVERDUE);
            exchangeService.submitReturnPhotos(exchange.getId(), reader.getId(), twoPhotos);
            assertThat(exchange.getStatus()).isEqualTo(ExchangeStatus.RETURN_PENDING);
        }

        @Test
        @DisplayName("повернути книгу до початку читання не можна")
        void beforeReading() {
            exchange.setStatus(ExchangeStatus.HANDOVER_PENDING);
            assertThatThrownBy(() -> exchangeService.submitReturnPhotos(exchange.getId(), reader.getId(), twoPhotos))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("власник підтверджує повернення, і лічильники оновлюються")
        void confirmReturn() {
            exchange.setStatus(ExchangeStatus.RETURN_PENDING);
            exchange.setReturnedAt(LocalDateTime.now());
            photo(reader, PhotoStage.RETURN);

            ExchangeResponse response = exchangeService.confirmReturn(exchange.getId(), owner.getId());

            assertThat(response.status()).isEqualTo(ExchangeStatus.COMPLETED);
            assertThat(exchange.getCompletedAt()).isNotNull();
            assertThat(exchange.getListing().getStatus()).isEqualTo(ListingStatus.AVAILABLE);
            assertThat(owner.getBooksGiven()).isEqualTo(1);
            assertThat(reader.getBooksTaken()).isEqualTo(1);
            assertThat(reader.getBooksReturnedOnTime()).isEqualTo(1);
            verify(exchangeNotifier).completed(exchange);
        }

        @Test
        @DisplayName("без фото читача підтвердити повернення не можна")
        void confirmNeedsReaderPhoto() {
            exchange.setStatus(ExchangeStatus.RETURN_PENDING);
            assertThatThrownBy(() -> exchangeService.confirmReturn(exchange.getId(), owner.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("фото");
        }

        @Test
        @DisplayName("поштою: підтвердження повернення вимагає відправленої накладної")
        void mailConfirmReturn() {
            exchange.setDeliveryMethod(DeliveryMethod.MAIL);
            exchange.setStatus(ExchangeStatus.RETURN_PENDING);
            photo(reader, PhotoStage.RETURN);
            assertThatThrownBy(() -> exchangeService.confirmReturn(exchange.getId(), owner.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Читач ще не відправив книгу назад");

            ShipmentInfo back = shipment(ShipmentDirection.TO_OWNER, ShipmentStatus.SHIPPED);
            exchangeService.confirmReturn(exchange.getId(), owner.getId());
            assertThat(back.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        }

        @Test
        @DisplayName("пізнє повернення рахується як прострочене")
        void lateReturnCountsOverdue() {
            exchange.setDeadline(LocalDate.now().minusDays(10));
            exchange.setReturnedAt(LocalDateTime.now());
            exchange.setCompletedAt(LocalDateTime.now().minusDays(1));

            exchangeService.finalizeReturnedExchange(exchange);

            assertThat(reader.getBooksOverdue()).isEqualTo(1);
            assertThat(reader.getBooksReturnedOnTime()).isZero();
            assertThat(exchange.getCompletedAt()).isBefore(LocalDateTime.now().minusHours(1));
        }

        @Test
        @DisplayName("без дати повернення рахується сьогоднішня дата")
        void returnedTodayByDefault() {
            exchangeService.finalizeReturnedExchange(exchange);
            assertThat(reader.getBooksReturnedOnTime()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("спір щодо повернення")
    class DisputeFlow {

        @BeforeEach
        void returnPending() {
            exchange.setStatus(ExchangeStatus.RETURN_PENDING);
            when(disputeRepository.save(any(Dispute.class))).thenAnswer(inv -> {
                Dispute d = inv.getArgument(0);
                d.setId(UUID.randomUUID());
                return d;
            });
        }

        @Test
        @DisplayName("учасник додає фото-доказ")
        void addEvidence() {
            exchangeService.addDisputePhoto(exchange.getId(), reader.getId(), new AddExchangePhotoRequest("/uploads/d.jpg", "подряпина"));
            ArgumentCaptor<ExchangePhoto> captor = ArgumentCaptor.forClass(ExchangePhoto.class);
            verify(exchangePhotoRepository).save(captor.capture());
            assertThat(captor.getValue().getStage()).isEqualTo(PhotoStage.DISPUTE);
            assertThat(captor.getValue().getUploadedBy()).isEqualTo(reader);
            assertThat(captor.getValue().getNote()).isEqualTo("подряпина");
        }

        @Test
        @DisplayName("власник теж може додати доказ")
        void ownerEvidence() {
            exchangeService.addDisputePhoto(exchange.getId(), owner.getId(), new AddExchangePhotoRequest("/uploads/d.jpg", null));
            verify(exchangePhotoRepository).save(any(ExchangePhoto.class));
        }

        @Test
        @DisplayName("сторонній користувач не може додати доказ")
        void strangerEvidence() {
            UUID stranger = UUID.randomUUID();
            assertThatThrownBy(() -> exchangeService.addDisputePhoto(exchange.getId(), stranger,
                    new AddExchangePhotoRequest("/uploads/d.jpg", null)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("доказ можна додати лише під час очікування повернення")
        void evidenceWrongStage() {
            exchange.setStatus(ExchangeStatus.IN_READING);
            assertThatThrownBy(() -> exchangeService.addDisputePhoto(exchange.getId(), owner.getId(),
                    new AddExchangePhotoRequest("/uploads/d.jpg", null)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("власник відкриває спір, читач отримує сповіщення")
        void openDispute() {
            ExchangeResponse response = exchangeService.openDispute(exchange.getId(), owner.getId(),
                    new OpenDisputeRequest("Книгу пошкоджено", List.of("/uploads/e.jpg")));

            assertThat(response.status()).isEqualTo(ExchangeStatus.DISPUTED);
            assertThat(exchange.getDisputeReason()).isEqualTo("Книгу пошкоджено");
            verify(disputeRepository).save(any(Dispute.class));
            verify(notificationService).notify(eq(reader), eq(NotificationType.DISPUTE_FILED), any(), anyString());
        }

        @Test
        @DisplayName("другий відкритий спір не створюється")
        void secondDispute() {
            when(disputeRepository.findByExchangeId(exchange.getId())).thenReturn(List.of(
                    Dispute.builder().id(UUID.randomUUID()).filedBy(owner).status(DisputeStatus.OPEN)
                            .createdAt(LocalDateTime.now()).build()));
            assertThatThrownBy(() -> exchangeService.openDispute(exchange.getId(), owner.getId(),
                    new OpenDisputeRequest("ще раз", List.of())))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("вже є відкритий спір");
        }
    }

    @Nested
    @DisplayName("продовження терміну")
    class Extension {

        private DeadlineExtensionRequest pending;

        @BeforeEach
        void reading() {
            exchange.setStatus(ExchangeStatus.IN_READING);
            exchange.setDeadline(LocalDate.of(2026, 11, 1));
            pending = DeadlineExtensionRequest.builder()
                    .id(UUID.randomUUID()).exchange(exchange).extraDays(7).status(ExtensionStatus.PENDING).build();
            when(extensionRequestRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        }

        @Test
        @DisplayName("читач просить продовження, власник отримує сповіщення")
        void request() {
            exchangeService.requestExtension(exchange.getId(), reader.getId(), new CreateExtensionRequest(7, "Не встигаю"));
            verify(extensionRequestRepository).save(any(DeadlineExtensionRequest.class));
            verify(notificationService).notify(eq(owner), eq(NotificationType.EXTENSION_REQUESTED), eq(exchange.getId()),
                    org.mockito.ArgumentMatchers.contains("7 дн."));
        }

        @Test
        @DisplayName("поки один запит не розглянуто, другий не приймається")
        void onePendingAtATime() {
            when(extensionRequestRepository.findByExchangeId(exchange.getId())).thenReturn(List.of(pending));
            assertThatThrownBy(() -> exchangeService.requestExtension(exchange.getId(), reader.getId(),
                    new CreateExtensionRequest(3, null)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("продовження можливе лише під час читання")
        void onlyWhileReading() {
            exchange.setStatus(ExchangeStatus.RETURN_PENDING);
            assertThatThrownBy(() -> exchangeService.requestExtension(exchange.getId(), reader.getId(),
                    new CreateExtensionRequest(3, null)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("без дати завершення читання продовжувати нічого")
        void noDeadline() {
            exchange.setStatus(ExchangeStatus.OVERDUE);
            exchange.setDeadline(null);
            assertThatThrownBy(() -> exchangeService.requestExtension(exchange.getId(), reader.getId(),
                    new CreateExtensionRequest(3, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ще не почався");
        }

        @Test
        @DisplayName("схвалення зсуває дату повернення")
        void approve() {
            exchangeService.decideExtension(exchange.getId(), pending.getId(), owner.getId(), true, "  Добре  ");
            assertThat(pending.getStatus()).isEqualTo(ExtensionStatus.APPROVED);
            assertThat(pending.getDecisionComment()).isEqualTo("Добре");
            assertThat(exchange.getExtendedDeadline()).isEqualTo(LocalDate.of(2026, 11, 8));
            assertThat(pending.getRequestedNewDeadline()).isEqualTo(LocalDate.of(2026, 11, 8));
            verify(exchangeNotifier).extensionDecided(exchange, true);
        }

        @Test
        @DisplayName("друге продовження рахується від уже продовженої дати")
        void approveAfterPreviousExtension() {
            exchange.setExtendedDeadline(LocalDate.of(2026, 11, 8));
            exchangeService.decideExtension(exchange.getId(), pending.getId(), owner.getId(), true, null);
            assertThat(exchange.getExtendedDeadline()).isEqualTo(LocalDate.of(2026, 11, 15));
        }

        @Test
        @DisplayName("відмова не змінює дату")
        void reject() {
            exchangeService.decideExtension(exchange.getId(), pending.getId(), owner.getId(), false, " ");
            assertThat(pending.getStatus()).isEqualTo(ExtensionStatus.REJECTED);
            assertThat(pending.getDecisionComment()).isNull();
            assertThat(exchange.getExtendedDeadline()).isNull();
            verify(exchangeNotifier).extensionDecided(exchange, false);
        }

        @Test
        @DisplayName("розглянутий запит не можна вирішити вдруге")
        void decidedTwice() {
            pending.setStatus(ExtensionStatus.APPROVED);
            assertThatThrownBy(() -> exchangeService.decideExtension(exchange.getId(), pending.getId(), owner.getId(), true, null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("запит іншого обміну відхиляється")
        void foreignRequest() {
            pending.setExchange(Exchange.builder().id(UUID.randomUUID()).build());
            assertThatThrownBy(() -> exchangeService.decideExtension(exchange.getId(), pending.getId(), owner.getId(), true, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("неіснуючий запит")
        void missingRequest() {
            assertThatThrownBy(() -> exchangeService.decideExtension(exchange.getId(), UUID.randomUUID(), owner.getId(), true, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("вирішує лише власник")
        void onlyOwnerDecides() {
            assertThatThrownBy(() -> exchangeService.decideExtension(exchange.getId(), pending.getId(), reader.getId(), true, null))
                    .isInstanceOf(AccessDeniedException.class);
            verify(extensionRequestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("доставка поштою")
    class Mail {

        private CreateShipmentRequest address(ShipmentDirection direction) {
            return new CreateShipmentRequest(direction, "Іван", "+380501112233", ShipmentCarrier.NOVA_POSHTA, "Рівне", "12");
        }

        @BeforeEach
        void mail() {
            exchange.setDeliveryMethod(DeliveryMethod.MAIL);
        }

        @Test
        @DisplayName("читач вказує відділення після фото власника")
        void readerAddsAddress() {
            photo(owner, PhotoStage.HANDOVER);
            exchangeService.createShipment(exchange.getId(), reader.getId(), address(ShipmentDirection.TO_READER));
            verify(shipmentInfoRepository).save(any(ShipmentInfo.class));
            verify(exchangeNotifier).shipmentAddressAdded(exchange, ShipmentDirection.TO_READER);
        }

        @Test
        @DisplayName("адресу до читача вказує лише читач")
        void wrongRecipient() {
            assertThatThrownBy(() -> exchangeService.createShipment(exchange.getId(), owner.getId(), address(ShipmentDirection.TO_READER)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("без фото власника адресу вказати не можна")
        void noOwnerPhoto() {
            assertThatThrownBy(() -> exchangeService.createShipment(exchange.getId(), reader.getId(), address(ShipmentDirection.TO_READER)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("особиста зустріч не передбачає відправлення")
        void pickupHasNoShipment() {
            exchange.setDeliveryMethod(DeliveryMethod.PICKUP);
            assertThatThrownBy(() -> exchangeService.createShipment(exchange.getId(), reader.getId(), address(ShipmentDirection.TO_READER)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("не передбачає");
        }

        @Test
        @DisplayName("друге відправлення в тому ж напрямку не створюється")
        void duplicate() {
            photo(owner, PhotoStage.HANDOVER);
            shipment(ShipmentDirection.TO_READER, ShipmentStatus.PENDING);
            assertThatThrownBy(() -> exchangeService.createShipment(exchange.getId(), reader.getId(), address(ShipmentDirection.TO_READER)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("вже створено");
        }

        @Test
        @DisplayName("очікування адреси для повернення ставить читання на паузу")
        void returnAddressPausesReading() {
            exchange.setStatus(ExchangeStatus.RETURN_PENDING);
            exchange.setReturnPreparedAt(LocalDateTime.now().minusDays(3));
            exchangeService.createShipment(exchange.getId(), owner.getId(), address(ShipmentDirection.TO_OWNER));
            assertThat(exchange.getReadingPausedDays()).isEqualTo(3);
            verify(exchangeNotifier).shipmentAddressAdded(exchange, ShipmentDirection.TO_OWNER);
        }

        @Test
        @DisplayName("адреса для повернення в той самий день паузи не додає")
        void returnAddressSameDay() {
            exchange.setStatus(ExchangeStatus.RETURN_PENDING);
            exchange.setReturnPreparedAt(LocalDateTime.now());
            exchangeService.createShipment(exchange.getId(), owner.getId(), address(ShipmentDirection.TO_OWNER));
            assertThat(exchange.getReadingPausedDays()).isZero();
        }

        @Test
        @DisplayName("адреса для повернення лише після фото повернення")
        void returnAddressTooEarly() {
            exchange.setStatus(ExchangeStatus.IN_READING);
            assertThatThrownBy(() -> exchangeService.createShipment(exchange.getId(), owner.getId(), address(ShipmentDirection.TO_OWNER)))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("власник додає накладну, книга позначається відправленою")
        void ownerShips() {
            ShipmentInfo toReader = shipment(ShipmentDirection.TO_READER, ShipmentStatus.PENDING);
            exchangeService.shipWaybill(exchange.getId(), toReader.getId(), owner.getId(), new ShipWaybillRequest("/uploads/w.jpg"));
            assertThat(toReader.getStatus()).isEqualTo(ShipmentStatus.SHIPPED);
            assertThat(toReader.getWaybillPhotoUrl()).isEqualTo("/uploads/w.jpg");
            assertThat(toReader.getShippedAt()).isNotNull();
            assertThat(exchange.getReturnedAt()).isNull();
            verify(exchangeNotifier).bookShipped(exchange, toReader);
        }

        @Test
        @DisplayName("відправлення назад фіксує дату повернення")
        void readerShipsBack() {
            exchange.setStatus(ExchangeStatus.RETURN_PENDING);
            ShipmentInfo back = shipment(ShipmentDirection.TO_OWNER, ShipmentStatus.PENDING);
            exchangeService.shipWaybill(exchange.getId(), back.getId(), reader.getId(), new ShipWaybillRequest("/uploads/w.jpg"));
            assertThat(exchange.getReturnedAt()).isEqualTo(back.getShippedAt());
        }

        @Test
        @DisplayName("накладну додає лише відправник")
        void wrongSender() {
            ShipmentInfo toReader = shipment(ShipmentDirection.TO_READER, ShipmentStatus.PENDING);
            assertThatThrownBy(() -> exchangeService.shipWaybill(exchange.getId(), toReader.getId(), reader.getId(),
                    new ShipWaybillRequest("/uploads/w.jpg")))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("повторно відправити не можна")
        void alreadyShipped() {
            ShipmentInfo toReader = shipment(ShipmentDirection.TO_READER, ShipmentStatus.SHIPPED);
            assertThatThrownBy(() -> exchangeService.shipWaybill(exchange.getId(), toReader.getId(), owner.getId(),
                    new ShipWaybillRequest("/uploads/w.jpg")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("вже позначено");
        }

        @Test
        @DisplayName("відправлення іншого обміну відхиляється")
        void foreignShipment() {
            ShipmentInfo foreign = ShipmentInfo.builder().id(UUID.randomUUID())
                    .exchange(Exchange.builder().id(UUID.randomUUID()).build())
                    .direction(ShipmentDirection.TO_READER).build();
            when(shipmentInfoRepository.findById(foreign.getId())).thenReturn(Optional.of(foreign));
            assertThatThrownBy(() -> exchangeService.shipWaybill(exchange.getId(), foreign.getId(), owner.getId(),
                    new ShipWaybillRequest("/uploads/w.jpg")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("неіснуюче відправлення")
        void missingShipment() {
            assertThatThrownBy(() -> exchangeService.shipWaybill(exchange.getId(), UUID.randomUUID(), owner.getId(),
                    new ShipWaybillRequest("/uploads/w.jpg")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
