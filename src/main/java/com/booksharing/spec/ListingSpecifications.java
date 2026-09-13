package com.booksharing.spec;

import com.booksharing.entity.Listing;
import com.booksharing.enums.ListingStatus;
import com.booksharing.service.ListingService;
import java.util.List;
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

    /**
     * IN-фільтр, не рівність - дозволяє обрати кілька жанрів одночасно
     * (наприклад, "Фантастика" І "Фентезі" разом, логіка АБО між ними).
     * cb.lower() з обох боків - регістронезалежне порівняння, як і було
     * до переходу на мультивибір.
     */
    public static Specification<Listing> hasGenre(List<String> genres) {
        return (root, query, cb) -> {
            if (genres == null || genres.isEmpty()) {
                return null;
            }
            List<String> lowerGenres = genres.stream().map(String::toLowerCase).toList();
            return cb.lower(root.get("bookCatalogEntry").get("genre")).in(lowerGenres);
        };
    }

    /**
     * Фільтрує за ЕФЕКТИВНим населеним пунктом - той самий COALESCE, що
     * рахує {@link com.booksharing.mapper.ListingMapper} (власний override
     * оголошення, якщо є, інакше - населений пункт власника). Інакше
     * оголошення з override загубилось би у фільтрі за старим містом
     * власника, хоча реально показується під новим.
     */
    public static Specification<Listing> hasSettlement(List<String> settlementNames) {
        return (root, query, cb) -> {
            if (settlementNames == null || settlementNames.isEmpty()) {
                return null;
            }
            List<String> lowerNames = settlementNames.stream().map(String::toLowerCase).toList();
            var effectiveSettlement = cb.coalesce(
                    root.<String>get("settlementName"), root.get("owner").<String>get("settlementName"));
            return cb.lower(effectiveSettlement).in(lowerNames);
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