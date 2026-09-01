package com.booksharing.model.bookcatalog;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookCatalogEntryRepository extends JpaRepository<BookCatalogEntry, UUID> {

    Optional<BookCatalogEntry> findByIsbn(String isbn);

    Optional<BookCatalogEntry> findByExternalId(String externalId);
}
