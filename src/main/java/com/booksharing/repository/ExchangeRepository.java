package com.booksharing.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.booksharing.entity.Exchange;
import com.booksharing.enums.ExchangeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRepository extends JpaRepository<Exchange, UUID> {

    Optional<Exchange> findByRequestId(UUID requestId);

    List<Exchange> findByOwnerId(UUID ownerId);
    List<Exchange> findByRequestIdIn(Collection<UUID> requestIds);
    List<Exchange> findByReaderId(UUID readerId);

    List<Exchange> findByStatus(ExchangeStatus status);

    List<Exchange> findByListingIdOrderByCreatedAtDesc(UUID listingId);

    List<Exchange> findByReaderIdAndStatus(UUID readerId, ExchangeStatus status);
}
