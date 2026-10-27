package com.booksharing.spec;

import com.booksharing.entity.Listing;
import com.booksharing.enums.ListingStatus;
import com.booksharing.service.ListingService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class ListingSpecifications {

    private ListingSpecifications() {
    }

    public static Specification<Listing> hasStatus(List<ListingStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) {
                return null;
            }
            return root.get("status").in(statuses);
        };
    }

    public static Specification<Listing> hasGenre(List<String> genres) {
        return (root, query, cb) -> {
            if (genres == null || genres.isEmpty()) {
                return null;
            }
            List<String> lowerGenres = genres.stream().map(String::toLowerCase).toList();
            return cb.lower(root.get("bookCatalogEntry").get("genre")).in(lowerGenres);
        };
    }

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

    public static Specification<Listing> excludeOwner(UUID excludedOwnerId) {
        return (root, query, cb) -> {
            if (excludedOwnerId == null) {
                return null;
            }
            return cb.notEqual(root.get("owner").get("id"), excludedOwnerId);
        };
    }

    public static Specification<Listing> hasOwner(UUID ownerId) {
        return (root, query, cb) -> cb.equal(root.get("owner").get("id"), ownerId);
    }

    public static Specification<Listing> notHidden() {
        return notHidden(true);
    }

    public static Specification<Listing> notHidden(boolean apply) {
        return (root, query, cb) -> apply ? cb.isNull(root.get("hiddenAt")) : null;
    }
}
