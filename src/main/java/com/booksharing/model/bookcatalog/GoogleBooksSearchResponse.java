package com.booksharing.model.bookcatalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Сирі DTO для десеріалізації відповіді Google Books API
 * (https://www.googleapis.com/books/v1/volumes?q=...). Google повертає
 * значно більше полів (selfLink, etag, saleInfo, accessInfo...), яких ми
 * не мапимо — {@code @JsonIgnoreProperties(ignoreUnknown = true)}
 * обов'язковий на кожному, інакше Jackson впаде на першому ж невідомому полі.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleBooksSearchResponse(List<GoogleBooksVolume> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoogleBooksVolume(String id, VolumeInfo volumeInfo) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record VolumeInfo(
                String title,
                List<String> authors,
                String description,
                List<String> categories,
                ImageLinks imageLinks,
                List<IndustryIdentifier> industryIdentifiers) {

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record ImageLinks(String thumbnail) {
            }

            @JsonIgnoreProperties(ignoreUnknown = true)
            public record IndustryIdentifier(String type, String identifier) {
            }
        }
    }
}