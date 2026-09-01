package com.booksharing.model.bookcatalog;

import com.booksharing.common.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookCatalogService {

    private final GoogleBooksClient googleBooksClient;
    private final BookCatalogEntryRepository bookCatalogEntryRepository;
    private final BookCatalogMapper bookCatalogMapper;

    public List<BookCatalogSearchResultResponse> search(String query) {
        return googleBooksClient.search(query);
    }

    public BookCatalogEntryResponse getById(UUID id) {
        BookCatalogEntry entry = bookCatalogEntryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запис каталогу не знайдено: " + id));
        return bookCatalogMapper.toResponse(entry);
    }

    /**
     * Знаходить уже закешований запис за ISBN чи externalId, або створює
     * новий. Так одна й та сама книга, знайдена через пошук кількома
     * різними користувачами, не плодить дублікати {@link BookCatalogEntry}.
     */
    @Transactional
    public BookCatalogEntryResponse resolve(ResolveBookCatalogEntryRequest request) {
        BookCatalogEntry entry = findExisting(request)
                .orElseGet(() -> createFromRequest(request));

        entry = bookCatalogEntryRepository.save(entry);
        return bookCatalogMapper.toResponse(entry);
    }

    private Optional<BookCatalogEntry> findExisting(ResolveBookCatalogEntryRequest request) {
        if (request.isbn() != null && !request.isbn().isBlank()) {
            Optional<BookCatalogEntry> byIsbn = bookCatalogEntryRepository.findByIsbn(request.isbn());
            if (byIsbn.isPresent()) {
                return byIsbn;
            }
        }
        if (request.externalId() != null && !request.externalId().isBlank()) {
            return bookCatalogEntryRepository.findByExternalId(request.externalId());
        }
        return Optional.empty();
    }

    private BookCatalogEntry createFromRequest(ResolveBookCatalogEntryRequest request) {
        boolean fromGoogleBooks = request.externalId() != null && !request.externalId().isBlank();
        return BookCatalogEntry.builder()
                .isbn(request.isbn())
                .title(request.title())
                .author(request.author())
                .description(request.description())
                .genre(request.genre())
                .coverUrl(request.coverUrl())
                .externalId(request.externalId())
                .source(fromGoogleBooks ? BookSource.GOOGLE_BOOKS : BookSource.MANUAL)
                .build();
    }
}