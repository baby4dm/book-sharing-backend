package com.booksharing.repository;

import java.util.Optional;
import java.util.UUID;

import com.booksharing.entity.BookCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCatalogEntryRepository extends JpaRepository<BookCatalogEntry, UUID> {

    Optional<BookCatalogEntry> findByIsbn(String isbn);

    Optional<BookCatalogEntry> findByExternalId(String externalId);
}
