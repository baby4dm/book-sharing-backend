package com.booksharing.spec;

import com.booksharing.entity.Request;
import com.booksharing.enums.DeliveryMethod;
import java.util.UUID;

import com.booksharing.enums.RequestStatus;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;

public final class RequestSpecifications {

    private RequestSpecifications() {
    }

    public static Specification<Request> receivedBy(UUID ownerId) {
        return (root, query, cb) ->
                cb.equal(root.get("listing").get("owner").get("id"), ownerId);
    }
    public static Specification<Request> hasStatus(RequestStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }
    public static Specification<Request> sentBy(UUID requesterId) {
        return (root, query, cb) ->
                cb.equal(root.get("requester").get("id"), requesterId);
    }

    public static Specification<Request> hasDeliveryMethod(DeliveryMethod method) {
        return (root, query, cb) ->
                method == null ? null : cb.equal(root.get("preferredDeliveryMethod"), method);
    }

    public static Specification<Request> matchesSearch(String search, boolean personIsOwner) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) {
                return null;
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";

            Expression<String> title = root.get("listing")
                    .get("bookCatalogEntry")
                    .<String>get("title");

            Expression<String> person = personIsOwner
                    ? root.get("listing").get("owner").<String>get("name")
                    : root.get("requester").<String>get("name");

            return cb.or(
                    cb.like(cb.lower(title), pattern),
                    cb.like(cb.lower(person), pattern));
        };
    }
}