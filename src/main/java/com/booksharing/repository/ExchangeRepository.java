package com.booksharing.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.booksharing.entity.Exchange;
import com.booksharing.enums.ExchangeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExchangeRepository
        extends JpaRepository<Exchange, UUID>, JpaSpecificationExecutor<Exchange> {

    @Override
    @EntityGraph(attributePaths = {
            "listing", "listing.bookCatalogEntry", "owner", "reader"})
    Page<Exchange> findAll(Specification<Exchange> spec, Pageable pageable);



    Optional<Exchange> findByRequestId(UUID requestId);

    List<Exchange> findByOwnerId(UUID ownerId);
    List<Exchange> findByRequestIdIn(Collection<UUID> requestIds);
    List<Exchange> findByReaderId(UUID readerId);

    List<Exchange> findByStatus(ExchangeStatus status);

    List<Exchange> findByListingIdOrderByCreatedAtDesc(UUID listingId);

    List<Exchange> findByReaderIdAndStatus(UUID readerId, ExchangeStatus status);
}
