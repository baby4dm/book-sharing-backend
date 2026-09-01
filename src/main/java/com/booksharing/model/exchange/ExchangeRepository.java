package com.booksharing.model.exchange;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeRepository extends JpaRepository<Exchange, UUID> {

    Optional<Exchange> findByRequestId(UUID requestId);

    List<Exchange> findByOwnerId(UUID ownerId);

    List<Exchange> findByReaderId(UUID readerId);

    List<Exchange> findByStatus(ExchangeStatus status);

    // повна історія обмінів конкретної книги (хто й коли її читав)
    List<Exchange> findByListingIdOrderByCreatedAtDesc(UUID listingId);

    // для GET /api/users/{id}/history - прочитані книги конкретного читача
    List<Exchange> findByReaderIdAndStatus(UUID readerId, ExchangeStatus status);
}
