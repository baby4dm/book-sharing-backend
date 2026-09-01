package com.booksharing.model.bookcatalog;

import com.booksharing.model.bookcatalog.GoogleBooksSearchResponse.GoogleBooksVolume;
import com.booksharing.model.bookcatalog.GoogleBooksSearchResponse.GoogleBooksVolume.VolumeInfo;
import com.booksharing.model.bookcatalog.GoogleBooksSearchResponse.GoogleBooksVolume.VolumeInfo.IndustryIdentifier;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Проксі-пошук до Google Books API — без API-ключа (базовий пошук не
 * потребує автентифікації). Результати НЕ зберігаються в БД тут: це
 * лише прев'ю для вибору користувачем; кешування в
 * {@code book_catalog_entries} відбувається окремим кроком через
 * {@link BookCatalogService#resolve}, коли користувач обрав конкретну книгу.
 */
@Component
public class GoogleBooksClient {

    private static final int MAX_RESULTS = 10;

    private final RestClient restClient;

    public GoogleBooksClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://www.googleapis.com/books/v1")
                .build();
    }

    public List<BookCatalogSearchResultResponse> search(String query) {
        GoogleBooksSearchResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/volumes")
                        .queryParam("q", query)
                        .queryParam("maxResults", MAX_RESULTS)
                        .build())
                .retrieve()
                .body(GoogleBooksSearchResponse.class);

        if (response == null || response.items() == null) {
            return List.of();
        }

        return response.items().stream()
                .filter(item -> item.volumeInfo() != null)
                .map(this::toSearchResult)
                .toList();
    }

    private BookCatalogSearchResultResponse toSearchResult(GoogleBooksVolume item) {
        VolumeInfo info = item.volumeInfo();
        String author = info.authors() == null ? null : String.join(", ", info.authors());
        String genre = (info.categories() == null || info.categories().isEmpty())
                ? null
                : info.categories().get(0);
        String coverUrl = info.imageLinks() == null ? null : info.imageLinks().thumbnail();

        return new BookCatalogSearchResultResponse(
                info.title(),
                author,
                info.description(),
                genre,
                coverUrl,
                extractIsbn(info.industryIdentifiers()),
                item.id());
    }

    // Google повертає і ISBN_10, і ISBN_13 в одному списку - віддаємо перевагу 13-значному
    private String extractIsbn(List<IndustryIdentifier> identifiers) {
        if (identifiers == null) {
            return null;
        }
        return identifiers.stream()
                .filter(i -> "ISBN_13".equals(i.type()))
                .map(IndustryIdentifier::identifier)
                .findFirst()
                .or(() -> identifiers.stream()
                        .filter(i -> "ISBN_10".equals(i.type()))
                        .map(IndustryIdentifier::identifier)
                        .findFirst())
                .orElse(null);
    }
}