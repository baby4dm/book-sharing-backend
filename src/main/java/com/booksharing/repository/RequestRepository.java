package com.booksharing.repository;

import com.booksharing.entity.Request;
import com.booksharing.enums.RequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface RequestRepository extends JpaRepository<Request, UUID>, JpaSpecificationExecutor<Request> {

    List<Request> findByListingIdOrderByCreatedAtAsc(UUID listingId);

    Optional<Request> findByListingIdAndStatus(UUID listingId, RequestStatus status);

    @Override
    @EntityGraph(attributePaths = {
            "listing", "listing.bookCatalogEntry", "listing.owner", "requester"})
    Page<Request> findAll(Specification<Request> spec, Pageable pageable);
}