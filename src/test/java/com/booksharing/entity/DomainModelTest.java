package com.booksharing.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.booksharing.dto.res.ChatEvent;
import com.booksharing.dto.res.ChatMessageResponse;
import com.booksharing.dto.res.NotificationEvent;
import com.booksharing.dto.res.RequestResponse;
import com.booksharing.enums.BookSource;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.ListingStatus;
import com.booksharing.enums.RequestSort;
import com.booksharing.enums.RequestStatus;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

@DisplayName("Модель предметної області")
class DomainModelTest {

    @Test
    @DisplayName("значення за замовчуванням нових сутностей")
    void defaults() {
        User user = User.builder().name("Новий").build();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.getRatingAvg()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(user.getBooksGiven()).isZero();

        assertThat(Listing.builder().build().getStatus()).isEqualTo(ListingStatus.AVAILABLE);
        assertThat(Listing.builder().build().getDeliveryMethods()).isEmpty();
        assertThat(Exchange.builder().build().getStatus()).isEqualTo(ExchangeStatus.HANDOVER_PENDING);
        assertThat(Request.builder().build().getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(BookCatalogEntry.builder().title("x").build().getSource()).isEqualTo(BookSource.MANUAL);
        assertThat(Notification.builder().build().getIsRead()).isFalse();
    }

    @Test
    @DisplayName("оголошення приховане, лише коли модератор вказав дату приховування")
    void listingHidden() {
        Listing listing = Listing.builder().build();
        assertThat(listing.isHidden()).isFalse();
        listing.setHiddenAt(LocalDateTime.now());
        assertThat(listing.isHidden()).isTrue();
    }

    @Test
    @DisplayName("без днів паузи дата повернення не зсувається")
    void dueDateWithNullPause() {
        Exchange exchange = Exchange.builder().deadline(java.time.LocalDate.of(2026, 1, 1)).readingPausedDays(null).build();
        assertThat(exchange.getDueDate()).isEqualTo(java.time.LocalDate.of(2026, 1, 1));
    }

    @Test
    @DisplayName("сортування заявок")
    void requestSort() {
        assertThat(RequestSort.NEWEST.toSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
        assertThat(RequestSort.OLDEST.toSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(RequestSort.DEADLINE.toSort().getOrderFor("readingDays")).isNotNull();
        assertThat(RequestSort.RATING.toSort().getOrderFor("requester.ratingAvg")).isNotNull();
    }

    @Test
    @DisplayName("події чату й сповіщень")
    void events() {
        UUID room = UUID.randomUUID();
        UUID user = UUID.randomUUID();
        ChatMessageResponse message = new ChatMessageResponse(UUID.randomUUID(), room, user, "Я", "Привіт", null, null);
        assertThat(ChatEvent.ofMessage(message).chatRoomId()).isEqualTo(room);
        assertThat(ChatEvent.ofRead(room, user, LocalDateTime.now()).type()).isEqualTo(ChatEvent.Type.READ);
        assertThat(ChatEvent.ofTyping(room, user).userId()).isEqualTo(user);
        assertThat(NotificationEvent.read(user, 2).unreadCount()).isEqualTo(2);
        assertThat(NotificationEvent.readAll().kind()).isEqualTo(NotificationEvent.Kind.READ_ALL);
    }

    @Test
    @DisplayName("відповідь заявки доповнюється id обміну")
    void requestWithExchange() {
        RequestResponse response = new RequestResponse(UUID.randomUUID(), null, null, null, null, null, null,
                14, null, null, RequestStatus.ACTIVE, null, null, null, null);
        UUID exchangeId = UUID.randomUUID();
        assertThat(response.withExchangeId(exchangeId).exchangeId()).isEqualTo(exchangeId);
        assertThat(response.withExchangeId(exchangeId).readingDays()).isEqualTo(14);
    }
}
