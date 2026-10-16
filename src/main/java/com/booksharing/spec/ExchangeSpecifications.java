package com.booksharing.spec;

import com.booksharing.entity.Exchange;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.ExchangeRole;
import com.booksharing.enums.ExchangeSort;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ExchangeSpecifications {

    private ExchangeSpecifications() {
    }

    public static Specification<Exchange> involving(UUID userId, ExchangeRole role) {
        return (root, query, cb) -> {
            var owner = cb.equal(root.get("owner").get("id"), userId);
            var reader = cb.equal(root.get("reader").get("id"), userId);
            if (role == ExchangeRole.OWNER) {
                return owner;
            }
            if (role == ExchangeRole.READER) {
                return reader;
            }
            return cb.or(owner, reader);
        };
    }

    public static Specification<Exchange> hasDeliveryMethod(DeliveryMethod method) {
        return (root, query, cb) ->
                method == null ? null : cb.equal(root.get("deliveryMethod"), method);
    }

    public static Specification<Exchange> hasStatus(ExchangeStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Exchange> matchesSearch(String search, UUID userId) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            Expression<String> title =
                    root.get("listing").get("bookCatalogEntry").<String>get("title");
            Expression<String> ownerName = root.get("owner").<String>get("name");
            Expression<String> readerName = root.get("reader").<String>get("name");
            return cb.or(
                    cb.like(cb.lower(title), pattern),
                    cb.and(
                            cb.notEqual(root.get("owner").get("id"), userId),
                            cb.like(cb.lower(ownerName), pattern)),
                    cb.and(
                            cb.notEqual(root.get("reader").get("id"), userId),
                            cb.like(cb.lower(readerName), pattern)));
        };
    }

    public static Specification<Exchange> sortedBy(ExchangeSort sort) {
        return (root, query, cb) -> {
            if (query == null
                    || Long.class.equals(query.getResultType())
                    || long.class.equals(query.getResultType())) {
                return null;
            }
            Expression<LocalDate> effectiveDeadline = cb.coalesce(
                    root.<LocalDate>get("extendedDeadline"),
                    root.<LocalDate>get("deadline"));
            Order byCreatedDesc = cb.desc(root.get("createdAt"));
            List<Order> orders = switch (sort == null ? ExchangeSort.DEADLINE : sort) {
                case DEADLINE -> List.of(cb.asc(effectiveDeadline), byCreatedDesc);
                case NEWEST -> List.of(byCreatedDesc);
                case OLDEST -> List.of(cb.asc(root.get("createdAt")));
            };
            query.orderBy(orders);
            return null;
        };
    }
}