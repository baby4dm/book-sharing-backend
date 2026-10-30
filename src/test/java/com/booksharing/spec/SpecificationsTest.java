package com.booksharing.spec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.booksharing.entity.Exchange;
import com.booksharing.entity.Listing;
import com.booksharing.entity.Request;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.ExchangeRole;
import com.booksharing.enums.ExchangeSort;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.ListingStatus;
import com.booksharing.enums.RequestStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.jpa.domain.Specification;

/**
 * Умови пошуку перевіряються на рівні «чи додається умова»: порожній фільтр
 * не повинен звужувати вибірку (null), заповнений повинен давати предикат.
 * Самі SQL-запити перевіряє Hibernate під час інтеграційного тесту.
 */
@SuppressWarnings("unchecked")
@DisplayName("Умови пошуку (JPA Specification)")
class SpecificationsTest {

    private final UUID userId = UUID.randomUUID();

    private static <T> Object apply(Specification<T> spec, CriteriaQuery<?> query) {
        Root<T> root = mock(Root.class, RETURNS_DEEP_STUBS);
        CriteriaBuilder cb = mock(CriteriaBuilder.class, RETURNS_DEEP_STUBS);
        return spec.toPredicate(root, query, cb);
    }

    private static <T> Object apply(Specification<T> spec) {
        return apply(spec, mock(CriteriaQuery.class));
    }

    @Nested
    @DisplayName("обміни")
    class Exchanges {

        @ParameterizedTest
        @EnumSource(ExchangeRole.class)
        @DisplayName("участь користувача за роллю")
        void involving(ExchangeRole role) {
            assertThat(apply(ExchangeSpecifications.involving(userId, role))).isNotNull();
        }

        @Test
        @DisplayName("фільтри доставки, статусу й пошуку")
        void filters() {
            assertThat(apply(ExchangeSpecifications.hasDeliveryMethod(null))).isNull();
            assertThat(apply(ExchangeSpecifications.hasDeliveryMethod(DeliveryMethod.MAIL))).isNotNull();
            assertThat(apply(ExchangeSpecifications.hasStatus(null))).isNull();
            assertThat(apply(ExchangeSpecifications.hasStatus(ExchangeStatus.OVERDUE))).isNotNull();
            assertThat(apply(ExchangeSpecifications.matchesSearch(null, userId))).isNull();
            assertThat(apply(ExchangeSpecifications.matchesSearch("  ", userId))).isNull();
            assertThat(apply(ExchangeSpecifications.matchesSearch(" Кобзар ", userId))).isNotNull();
        }

        @ParameterizedTest
        @EnumSource(ExchangeSort.class)
        @DisplayName("сортування задає порядок у запиті, але не умову")
        void sorting(ExchangeSort sort) {
            CriteriaQuery<Exchange> query = mock(CriteriaQuery.class);
            doReturn(Exchange.class).when(query).getResultType();
            assertThat(apply(ExchangeSpecifications.sortedBy(sort), query)).isNull();
            verify(query).orderBy(anyList());
        }

        @Test
        @DisplayName("без сортування використовується термін, у запитах кількості порядок не задається")
        void sortingDefaultsAndCount() {
            CriteriaQuery<Exchange> query = mock(CriteriaQuery.class);
            doReturn(Exchange.class).when(query).getResultType();
            apply(ExchangeSpecifications.sortedBy(null), query);
            verify(query).orderBy(anyList());

            CriteriaQuery<Long> count = mock(CriteriaQuery.class);
            doReturn(Long.class).when(count).getResultType();
            assertThat(apply(ExchangeSpecifications.sortedBy(ExchangeSort.NEWEST), count)).isNull();
            verify(count, never()).orderBy(anyList());

            CriteriaQuery<Long> primitiveCount = mock(CriteriaQuery.class);
            doReturn(long.class).when(primitiveCount).getResultType();
            apply(ExchangeSpecifications.sortedBy(ExchangeSort.NEWEST), primitiveCount);
            verify(primitiveCount, never()).orderBy(anyList());

            assertThat(apply(ExchangeSpecifications.sortedBy(ExchangeSort.OLDEST), null)).isNull();
        }
    }

    @Nested
    @DisplayName("оголошення")
    class Listings {

        @Test
        @DisplayName("порожні фільтри не звужують каталог")
        void emptyFilters() {
            assertThat(apply(ListingSpecifications.hasStatus(null))).isNull();
            assertThat(apply(ListingSpecifications.hasStatus(List.of()))).isNull();
            assertThat(apply(ListingSpecifications.hasGenre(null))).isNull();
            assertThat(apply(ListingSpecifications.hasGenre(List.of()))).isNull();
            assertThat(apply(ListingSpecifications.hasSettlement(null))).isNull();
            assertThat(apply(ListingSpecifications.hasSettlement(List.of()))).isNull();
            assertThat(apply(ListingSpecifications.matchesSearch(null))).isNull();
            assertThat(apply(ListingSpecifications.matchesSearch(" "))).isNull();
            assertThat(apply(ListingSpecifications.excludeOwner(null))).isNull();
            assertThat(apply(ListingSpecifications.notHidden(false))).isNull();
        }

        @Test
        @DisplayName("заповнені фільтри дають умови")
        void filledFilters() {
            assertThat(apply(ListingSpecifications.hasStatus(List.of(ListingStatus.AVAILABLE)))).isNotNull();
            assertThat(apply(ListingSpecifications.hasGenre(List.of("Поезія")))).isNotNull();
            assertThat(apply(ListingSpecifications.hasSettlement(List.of("Рівне")))).isNotNull();
            assertThat(apply(ListingSpecifications.matchesSearch("Франко"))).isNotNull();
            assertThat(apply(ListingSpecifications.excludeOwner(userId))).isNotNull();
            assertThat(apply(ListingSpecifications.hasOwner(userId))).isNotNull();
            assertThat(apply(ListingSpecifications.notHidden())).isNotNull();
        }
    }

    @Nested
    @DisplayName("заявки")
    class Requests {

        @Test
        @DisplayName("власник, читач, статус, доставка й пошук")
        void filters() {
            assertThat(apply(RequestSpecifications.receivedBy(userId))).isNotNull();
            assertThat(apply(RequestSpecifications.sentBy(userId))).isNotNull();
            assertThat(apply(RequestSpecifications.hasStatus(null))).isNull();
            assertThat(apply(RequestSpecifications.hasStatus(RequestStatus.ACTIVE))).isNotNull();
            assertThat(apply(RequestSpecifications.hasDeliveryMethod(null))).isNull();
            assertThat(apply(RequestSpecifications.hasDeliveryMethod(DeliveryMethod.PICKUP))).isNotNull();
            assertThat(apply(RequestSpecifications.matchesSearch(null, true))).isNull();
            assertThat(apply(RequestSpecifications.matchesSearch("Іван", true))).isNotNull();
            assertThat(apply(RequestSpecifications.matchesSearch("Іван", false))).isNotNull();
        }
    }

    @Test
    @DisplayName("типи сутностей у сигнатурах")
    void entityTypes() {
        Specification<Listing> listing = ListingSpecifications.notHidden();
        Specification<Request> request = RequestSpecifications.sentBy(userId);
        assertThat(listing).isNotNull();
        assertThat(request).isNotNull();
    }
}
