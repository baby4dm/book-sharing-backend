package com.booksharing.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.booksharing.entity.Request;
import com.booksharing.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequestRepository extends JpaRepository<Request, UUID> {

    // повна черга заявок на оголошення, у порядку подання - саме так
    // власник має бачити всіх заявників одночасно (не тільки свою позицію)
    List<Request> findByListingIdOrderByCreatedAtAsc(UUID listingId);

    List<Request> findByRequesterId(UUID requesterId);

    // на listing може бути щонайбільше одна ACTIVE заявка одночасно
    Optional<Request> findByListingIdAndStatus(UUID listingId, RequestStatus status);

    // "вхідні" заявки для власника - на всі ЙОГО оголошення разом, не на
    // одне конкретне. Underscore-нотація (Listing_Owner_Id) traverse'ить
    // Request -> listing -> owner -> id через вкладені зв'язки, Spring
    // Data сам генерує потрібний JOIN із назви методу.
    List<Request> findByListing_Owner_IdOrderByCreatedAtDesc(UUID ownerId);
}