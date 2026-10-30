package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.common.exception.AccountRestrictedException;
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
import com.booksharing.support.TestData;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Заявки на книги")
class RequestServiceTest {

    @Mock private RequestRepository requestRepository;
    @Mock private ListingRepository listingRepository;
    @Mock private ExchangeRepository exchangeRepository;
    @Mock private ChatRoomRepository chatRoomRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private AccountGuard accountGuard;
    @Spy private RequestMapper requestMapper = Mappers.getMapper(RequestMapper.class);

    @InjectMocks
    private RequestService requestService;

    private User owner;
    private User reader;
    private Listing listing;

    @BeforeEach
    void setUp() {
        owner = TestData.user("Власник");
        reader = TestData.user("Читач");
        listing = TestData.listing(owner);
        when(listingRepository.findById(listing.getId())).thenReturn(Optional.of(listing));
        when(userRepository.findById(reader.getId())).thenReturn(Optional.of(reader));
        when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
        when(requestRepository.save(any(Request.class))).thenAnswer(inv -> {
            Request r = inv.getArgument(0);
            if (r.getId() == null) {
                r.setId(UUID.randomUUID());
            }
            return r;
        });
        when(exchangeRepository.save(any(Exchange.class))).thenAnswer(inv -> {
            Exchange e = inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
    }

    private CreateRequestRequest body(String method) {
        return new CreateRequestRequest(14, method, "Прочитаю за два тижні");
    }

    @Nested
    @DisplayName("подання заявки")
    class Submit {

        @Test
        @DisplayName("створює заявку зі статусом PENDING і сповіщає власника")
        void createsPendingRequest() {
            when(requestRepository.findByListingIdOrderByCreatedAtAsc(listing.getId())).thenReturn(List.of());

            RequestResponse response = requestService.submit(listing.getId(), reader.getId(), body("MAIL"));

            assertThat(response.status()).isEqualTo(RequestStatus.PENDING);
            assertThat(response.preferredDeliveryMethod()).isEqualTo(DeliveryMethod.MAIL);
            assertThat(response.requesterId()).isEqualTo(reader.getId());
            assertThat(response.listingBookTitle()).isEqualTo("Лісова пісня");
            verify(accountGuard).requireCanCreateContent(reader.getId());
            verify(notificationService).notify(eq(owner), eq(NotificationType.NEW_REQUEST), any(), anyString());
        }

        @Test
        @DisplayName("відхиляє заявку, якщо оголошення не існує")
        void listingNotFound() {
            UUID missing = UUID.randomUUID();
            assertThatThrownBy(() -> requestService.submit(missing, reader.getId(), body("PICKUP")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("відхиляє заявку від неіснуючого користувача")
        void requesterNotFound() {
            UUID missing = UUID.randomUUID();
            assertThatThrownBy(() -> requestService.submit(listing.getId(), missing, body("PICKUP")))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("не приймає заявки на архівне оголошення")
        void archivedListing() {
            listing.setStatus(ListingStatus.ARCHIVED);
            assertThatThrownBy(() -> requestService.submit(listing.getId(), reader.getId(), body("PICKUP")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("знято з публікації");
        }

        @Test
        @DisplayName("не приймає заявки на приховане оголошення")
        void hiddenListing() {
            listing.setHiddenAt(LocalDateTime.now());
            assertThatThrownBy(() -> requestService.submit(listing.getId(), reader.getId(), body("PICKUP")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("приховано");
        }

        @Test
        @DisplayName("власник не може подати заявку на свою книгу")
        void ownBook() {
            assertThatThrownBy(() -> requestService.submit(listing.getId(), owner.getId(), body("PICKUP")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("невідомий спосіб доставки")
        void unknownDelivery() {
            assertThatThrownBy(() -> requestService.submit(listing.getId(), reader.getId(), body("DRONE")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Невідомий спосіб доставки");
        }

        @Test
        @DisplayName("спосіб доставки, який оголошення не підтримує")
        void unsupportedDelivery() {
            listing.setDeliveryMethods(List.of("PICKUP"));
            assertThatThrownBy(() -> requestService.submit(listing.getId(), reader.getId(), body("MAIL")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("не підтримує");
        }

        @Test
        @DisplayName("друга заявка, поки перша на розгляді, відхиляється")
        void duplicatePending() {
            when(requestRepository.findByListingIdOrderByCreatedAtAsc(listing.getId()))
                    .thenReturn(List.of(TestData.request(listing, reader, RequestStatus.PENDING)));
            assertThatThrownBy(() -> requestService.submit(listing.getId(), reader.getId(), body("PICKUP")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("уже є активна заявка");
        }

        @Test
        @DisplayName("схвалена заявка блокує нову, поки книга в обміні")
        void activeWhileBusy() {
            listing.setStatus(ListingStatus.IN_EXCHANGE);
            when(requestRepository.findByListingIdOrderByCreatedAtAsc(listing.getId()))
                    .thenReturn(List.of(TestData.request(listing, reader, RequestStatus.ACTIVE)));
            assertThatThrownBy(() -> requestService.submit(listing.getId(), reader.getId(), body("PICKUP")))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("після завершеного обміну можна подати заявку знову")
        void activeButBookIsFree() {
            when(requestRepository.findByListingIdOrderByCreatedAtAsc(listing.getId()))
                    .thenReturn(List.of(
                            TestData.request(listing, reader, RequestStatus.ACTIVE),
                            TestData.request(listing, TestData.user("Інший"), RequestStatus.PENDING)));
            RequestResponse response = requestService.submit(listing.getId(), reader.getId(), body("PICKUP"));
            assertThat(response.status()).isEqualTo(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("обмежений користувач не може подати заявку")
        void restrictedUser() {
            org.mockito.Mockito.doThrow(new AccountRestrictedException("обмежено"))
                    .when(accountGuard).requireCanCreateContent(reader.getId());
            assertThatThrownBy(() -> requestService.submit(listing.getId(), reader.getId(), body("PICKUP")))
                    .isInstanceOf(AccountRestrictedException.class);
            verify(requestRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("схвалення заявки")
    class Approve {

        private Request pending;

        @BeforeEach
        void setUp() {
            pending = TestData.request(listing, reader, RequestStatus.PENDING);
            when(requestRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
            when(chatRoomRepository.findBetween(owner.getId(), reader.getId())).thenReturn(Optional.empty());
        }

        @Test
        @DisplayName("створює обмін, резервує книгу й відкриває чат")
        void createsExchange() {
            RequestResponse response = requestService.approve(pending.getId(), owner.getId());

            assertThat(response.status()).isEqualTo(RequestStatus.ACTIVE);
            assertThat(response.exchangeId()).isNotNull();
            assertThat(pending.getDecidedAt()).isNotNull();
            assertThat(listing.getStatus()).isEqualTo(ListingStatus.RESERVED);

            ArgumentCaptor<Exchange> captor = ArgumentCaptor.forClass(Exchange.class);
            verify(exchangeRepository).save(captor.capture());
            Exchange created = captor.getValue();
            assertThat(created.getStatus()).isEqualTo(ExchangeStatus.HANDOVER_PENDING);
            assertThat(created.getOwner()).isEqualTo(owner);
            assertThat(created.getReader()).isEqualTo(reader);
            assertThat(created.getReadingDays()).isEqualTo(14);
            verify(chatRoomRepository).save(any(ChatRoom.class));
            verify(notificationService).notify(eq(reader), eq(NotificationType.REQUEST_APPROVED), any(), anyString());
        }

        @Test
        @DisplayName("не створює другий чат, якщо учасники вже листувалися")
        void reusesChat() {
            when(chatRoomRepository.findBetween(owner.getId(), reader.getId()))
                    .thenReturn(Optional.of(ChatRoom.builder().id(UUID.randomUUID()).build()));
            requestService.approve(pending.getId(), owner.getId());
            verify(chatRoomRepository, never()).save(any());
        }

        @Test
        @DisplayName("завершений обмін не заважає схвалити нову заявку")
        void completedExchangeDoesNotBlock() {
            when(exchangeRepository.existsByListingIdAndStatusNot(listing.getId(), ExchangeStatus.COMPLETED))
                    .thenReturn(false);
            RequestResponse response = requestService.approve(pending.getId(), owner.getId());
            assertThat(response.status()).isEqualTo(RequestStatus.ACTIVE);
        }

        @Test
        @DisplayName("незавершений обмін блокує схвалення")
        void activeExchangeBlocks() {
            when(exchangeRepository.existsByListingIdAndStatusNot(listing.getId(), ExchangeStatus.COMPLETED))
                    .thenReturn(true);
            assertThatThrownBy(() -> requestService.approve(pending.getId(), owner.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("в іншому обміні");
            assertThat(pending.getStatus()).isEqualTo(RequestStatus.PENDING);
        }

        @Test
        @DisplayName("схвалити може лише власник оголошення")
        void onlyOwner() {
            assertThatThrownBy(() -> requestService.approve(pending.getId(), reader.getId()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("розглянуту заявку не можна схвалити вдруге")
        void alreadyDecided() {
            pending.setStatus(RequestStatus.REJECTED);
            assertThatThrownBy(() -> requestService.approve(pending.getId(), owner.getId()))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Заявку вже розглянуто");
        }

        @Test
        @DisplayName("неіснуюча заявка")
        void notFound() {
            assertThatThrownBy(() -> requestService.approve(UUID.randomUUID(), owner.getId()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("відхилення заявки")
    class Reject {

        @Test
        @DisplayName("зберігає коментар і сповіщає читача")
        void rejects() {
            Request pending = TestData.request(listing, reader, RequestStatus.PENDING);
            when(requestRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

            RequestResponse response = requestService.reject(
                    pending.getId(), owner.getId(), new RejectRequestRequest("Книга вже обіцяна"));

            assertThat(response.status()).isEqualTo(RequestStatus.REJECTED);
            assertThat(response.rejectComment()).isEqualTo("Книга вже обіцяна");
            verify(notificationService).notify(eq(reader), eq(NotificationType.REQUEST_REJECTED), any(),
                    org.mockito.ArgumentMatchers.contains("Книга вже обіцяна"));
        }

        @Test
        @DisplayName("розглянуту заявку не можна відхилити")
        void alreadyDecided() {
            Request active = TestData.request(listing, reader, RequestStatus.ACTIVE);
            when(requestRepository.findById(active.getId())).thenReturn(Optional.of(active));
            assertThatThrownBy(() -> requestService.reject(active.getId(), owner.getId(), new RejectRequestRequest("ні")))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("чужу заявку відхилити не можна")
        void notOwner() {
            Request pending = TestData.request(listing, reader, RequestStatus.PENDING);
            when(requestRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
            assertThatThrownBy(() -> requestService.reject(pending.getId(), reader.getId(), new RejectRequestRequest("ні")))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("скасування заявки")
    class Cancel {

        @Test
        @DisplayName("читач скасовує свою заявку")
        void cancels() {
            Request pending = TestData.request(listing, reader, RequestStatus.PENDING);
            when(requestRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
            requestService.cancel(pending.getId(), reader.getId());
            assertThat(pending.getStatus()).isEqualTo(RequestStatus.CANCELLED);
            assertThat(pending.getDecidedAt()).isNotNull();
        }

        @Test
        @DisplayName("чужу заявку скасувати не можна")
        void notRequester() {
            Request pending = TestData.request(listing, reader, RequestStatus.PENDING);
            when(requestRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
            assertThatThrownBy(() -> requestService.cancel(pending.getId(), owner.getId()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("розглянуту заявку скасувати не можна")
        void decided() {
            Request active = TestData.request(listing, reader, RequestStatus.ACTIVE);
            when(requestRepository.findById(active.getId())).thenReturn(Optional.of(active));
            assertThatThrownBy(() -> requestService.cancel(active.getId(), reader.getId()))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("списки заявок")
    class Lists {

        @Test
        @DisplayName("черга оголошення містить id обміну для схваленої заявки")
        void queueWithExchangeIds() {
            Request active = TestData.request(listing, reader, RequestStatus.ACTIVE);
            Request pending = TestData.request(listing, TestData.user("Інший"), RequestStatus.PENDING);
            Exchange exchange = Exchange.builder().id(UUID.randomUUID()).request(active).build();
            when(requestRepository.findByListingIdOrderByCreatedAtAsc(listing.getId()))
                    .thenReturn(List.of(active, pending));
            when(exchangeRepository.findByRequestIdIn(any())).thenReturn(List.of(exchange));

            List<RequestResponse> queue = requestService.getQueueForListing(listing.getId());

            assertThat(queue).hasSize(2);
            assertThat(queue.get(0).exchangeId()).isEqualTo(exchange.getId());
            assertThat(queue.get(1).exchangeId()).isNull();
        }

        @Test
        @DisplayName("порожня черга не звертається до обмінів")
        void emptyQueue() {
            when(requestRepository.findByListingIdOrderByCreatedAtAsc(listing.getId())).thenReturn(List.of());
            assertThat(requestService.getQueueForListing(listing.getId())).isEmpty();
            verify(exchangeRepository, never()).findByRequestIdIn(any());
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("надіслані заявки повертаються з лічильниками за статусами")
        void myRequests() {
            Request pending = TestData.request(listing, reader, RequestStatus.PENDING);
            when(requestRepository.count(any(Specification.class))).thenReturn(2L);
            when(requestRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(pending), PageRequest.of(0, 10), 1));

            RequestListResponse result = requestService.getMyRequests(
                    reader.getId(), "пісня", DeliveryMethod.PICKUP, RequestStatus.PENDING, RequestSort.OLDEST, 0, 10);

            assertThat(result.items()).hasSize(1);
            assertThat(result.statusCounts()).hasSize(RequestStatus.values().length);
            assertThat(result.total()).isEqualTo(2L * RequestStatus.values().length);
            assertThat(result.totalElements()).isEqualTo(1);
        }

        @Test
        @SuppressWarnings("unchecked")
        @DisplayName("отримані заявки: некоректні сторінка й розмір виправляються")
        void receivedRequests() {
            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            when(requestRepository.findAll(any(Specification.class), pageable.capture()))
                    .thenReturn(new PageImpl<>(List.of()));

            RequestListResponse result = requestService.getReceivedRequests(
                    owner.getId(), null, null, null, null, -3, 500);

            assertThat(result.items()).isEmpty();
            assertThat(pageable.getValue().getPageNumber()).isZero();
            assertThat(pageable.getValue().getPageSize()).isEqualTo(50);
        }
    }
}