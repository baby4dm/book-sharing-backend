package com.booksharing.repository;

import java.util.List;
import java.util.UUID;

import com.booksharing.entity.Listing;
import com.booksharing.enums.ListingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

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

    /**
     * Тільки ті населені пункти, де РЕАЛЬНО є хоч одне оголошення - не
     * просто всі міста з профілів users (там можуть бути люди взагалі
     * без жодного оголошення, і фільтр показував би "мертву" опцію).
     * COALESCE - той самий override-пріоритет, що й у {@link
     * com.booksharing.spec.ListingSpecifications#hasSettlement}.
     */
    @Query(value = "SELECT DISTINCT COALESCE(l.settlement_name, u.settlement_name) "
            + "FROM listings l JOIN users u ON l.owner_id = u.id "
            + "WHERE COALESCE(l.settlement_name, u.settlement_name) IS NOT NULL "
            + "ORDER BY 1", nativeQuery = true)
    List<String> findDistinctSettlementNames();
}