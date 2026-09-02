package com.booksharing.spec;

import com.booksharing.entity.Listing;
import com.booksharing.enums.ListingStatus;
import com.booksharing.service.ListingService;
import org.springframework.data.jpa.domain.Specification;

/**
 * Фільтр за способом доставки навмисно НЕ тут: {@code deliveryMethods} -
 * це {@code text[]}, а перевірка "чи містить масив значення" через
 * Criteria API для нативних Postgres-масивів не має чистого рішення без
 * додаткових бібліотек (та сама категорія проблем, що й з enum-масивами
 * раніше). Для MVP-масштабу даних {@link ListingService} фільтрує за
 * способом доставки вже в Java після вибірки з БД - прийнятно для
 * навчального обсягу, за потреби можна замінити на native query пізніше.
 */
public final class ListingSpecifications {

    private ListingSpecifications() {
    }

    public static Specification<Listing> hasStatus(ListingStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Listing> hasGenre(String genre) {
        return (root, query, cb) -> {
            if (genre == null || genre.isBlank()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("bookCatalogEntry").get("genre")), genre.toLowerCase());
        };
    }

    public static Specification<Listing> hasOwnerCity(String city) {
        return (root, query, cb) -> {
            if (city == null || city.isBlank()) {
                return null;
            }
            return cb.equal(cb.lower(root.get("owner").get("city")), city.toLowerCase());
        };
    }

    public static Specification<Listing> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("bookCatalogEntry").get("title")), pattern),
                    cb.like(cb.lower(root.get("bookCatalogEntry").get("author")), pattern));
        };
    }
}