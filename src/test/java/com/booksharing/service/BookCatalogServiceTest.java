package com.booksharing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.booksharing.client.GoogleBooksClient;
import com.booksharing.common.exception.ResourceNotFoundException;
import com.booksharing.dto.req.ResolveBookCatalogEntryRequest;
import com.booksharing.dto.res.BookCatalogEntryResponse;
import com.booksharing.dto.res.BookCatalogSearchResultResponse;
import com.booksharing.entity.BookCatalogEntry;
import com.booksharing.enums.BookSource;
import com.booksharing.mapper.BookCatalogMapper;
import com.booksharing.repository.BookCatalogEntryRepository;
import com.booksharing.support.TestData;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Каталог книг")
class BookCatalogServiceTest {

    @Mock private GoogleBooksClient googleBooksClient;
    @Mock private BookCatalogEntryRepository bookCatalogEntryRepository;
    @Spy private BookCatalogMapper bookCatalogMapper = Mappers.getMapper(BookCatalogMapper.class);

    @InjectMocks
    private BookCatalogService bookCatalogService;

    @BeforeEach
    void setUp() {
        when(bookCatalogEntryRepository.save(any(BookCatalogEntry.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static ResolveBookCatalogEntryRequest request(String isbn, String externalId) {
        return new ResolveBookCatalogEntryRequest("Тіні забутих предків", isbn, "Михайло Коцюбинський",
                "Повість", "Класика", "https://covers/1.jpg", externalId);
    }

    @Test
    @DisplayName("пошук делегується Google Books")
    void search() {
        List<BookCatalogSearchResultResponse> results = List.of(
                new BookCatalogSearchResultResponse("Тіні", "Коцюбинський", null, null, null, null, "g1"));
        when(googleBooksClient.search("Тіні")).thenReturn(results);
        assertThat(bookCatalogService.search("Тіні")).isEqualTo(results);
    }

    @Test
    @DisplayName("запис за id")
    void getById() {
        BookCatalogEntry entry = TestData.book("Кобзар");
        when(bookCatalogEntryRepository.findById(entry.getId())).thenReturn(Optional.of(entry));
        assertThat(bookCatalogService.getById(entry.getId()).title()).isEqualTo("Кобзар");
        assertThatThrownBy(() -> bookCatalogService.getById(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("наявний запис знаходиться за ISBN")
    void existingByIsbn() {
        BookCatalogEntry existing = TestData.book("Тіні");
        when(bookCatalogEntryRepository.findByIsbn("978-966")).thenReturn(Optional.of(existing));

        BookCatalogEntryResponse response = bookCatalogService.resolve(request("978-966", "g1"));

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(bookCatalogEntryRepository, never()).findByExternalId(any());
    }

    @Test
    @DisplayName("якщо ISBN не знайдено, шукається за id Google Books")
    void existingByExternalId() {
        BookCatalogEntry existing = TestData.book("Тіні");
        when(bookCatalogEntryRepository.findByIsbn("978-966")).thenReturn(Optional.empty());
        when(bookCatalogEntryRepository.findByExternalId("g1")).thenReturn(Optional.of(existing));
        assertThat(bookCatalogService.resolve(request("978-966", "g1")).id()).isEqualTo(existing.getId());
    }

    @Test
    @DisplayName("нова книга з Google Books")
    void newFromGoogle() {
        BookCatalogEntryResponse response = bookCatalogService.resolve(request(" ", "g2"));
        assertThat(response.source()).isEqualTo(BookSource.GOOGLE_BOOKS);
        assertThat(response.title()).isEqualTo("Тіні забутих предків");
        assertThat(response.externalId()).isEqualTo("g2");
    }

    @Test
    @DisplayName("книга, введена вручну")
    void newManual() {
        BookCatalogEntryResponse response = bookCatalogService.resolve(request(null, " "));
        assertThat(response.source()).isEqualTo(BookSource.MANUAL);
        assertThat(bookCatalogService.resolve(request(null, null)).source()).isEqualTo(BookSource.MANUAL);
        verify(bookCatalogEntryRepository, never()).findByIsbn(any());
    }
}
