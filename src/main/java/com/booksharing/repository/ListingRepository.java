package com.booksharing.repository;

import java.util.List;
import java.util.UUID;

import com.booksharing.entity.Listing;
import com.booksharing.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * {@link JpaSpecificationExecutor} додано для GET /api/listings з
 * фільтрами (жанр, місто, спосіб доставки, статус, пошук) — сервіс
 * будує {@code Specification<Listing>} динамічно з query-параметрів,
 * замість окремого derived-методу на кожну комбінацію фільтрів.
 */
public interface ListingRepository extends JpaRepository<Listing, UUID>,
        JpaSpecificationExecutor<Listing> {

    List<Listing> findByOwnerId(UUID ownerId);

    List<Listing> findByStatus(ListingStatus status);
}
