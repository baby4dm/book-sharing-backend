package com.booksharing.model.request;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<Request, UUID> {

    // повна черга заявок на оголошення, у порядку подання - саме так
    // власник має бачити всіх заявників одночасно (не тільки свою позицію)
    List<Request> findByListingIdOrderByCreatedAtAsc(UUID listingId);

    List<Request> findByRequesterId(UUID requesterId);

    // на listing може бути щонайбільше одна ACTIVE заявка одночасно
    Optional<Request> findByListingIdAndStatus(UUID listingId, RequestStatus status);
}
