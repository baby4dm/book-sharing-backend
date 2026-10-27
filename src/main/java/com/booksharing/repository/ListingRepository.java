package com.booksharing.repository;

import java.util.List;
import java.util.UUID;

import com.booksharing.entity.Listing;
import com.booksharing.enums.ListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ListingRepository extends JpaRepository<Listing, UUID>,
        JpaSpecificationExecutor<Listing> {

    List<Listing> findByOwnerId(UUID ownerId);

    List<Listing> findByStatus(ListingStatus status);

    @EntityGraph(attributePaths = {"owner", "bookCatalogEntry"})
    Page<Listing> findByHiddenAtIsNotNull(Pageable pageable);

    long countByHiddenAtIsNotNull();


    @Query(value = "SELECT DISTINCT COALESCE(l.settlement_name, u.settlement_name) "
            + "FROM listings l JOIN users u ON l.owner_id = u.id "
            + "WHERE COALESCE(l.settlement_name, u.settlement_name) IS NOT NULL "
            + "ORDER BY 1", nativeQuery = true)
    List<String> findDistinctSettlementNames();
}