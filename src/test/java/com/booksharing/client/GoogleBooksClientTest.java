package com.booksharing.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.booksharing.dto.res.BookCatalogSearchResultResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Клієнт Google Books")
class GoogleBooksClientTest {

    private MockRestServiceServer server;
    private GoogleBooksClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new GoogleBooksClient(builder, "test-key");
    }

    private void respond(String json) {
        server.expect(requestTo(startsWith("https://www.googleapis.com/books/v1/volumes")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("q", "kobzar"))
                .andExpect(queryParam("maxResults", "10"))
                .andExpect(queryParam("key", "test-key"))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("перетворює відповідь API на результати пошуку")
    void mapsVolumes() {
        respond("""
                {"items": [
                  {"id": "g1", "volumeInfo": {
                    "title": "Kobzar", "authors": ["Taras Shevchenko", "Editor"], "description": "Poems",
                    "categories": ["Poetry", "Classics"], "imageLinks": {"thumbnail": "https://img/1.jpg"},
                    "industryIdentifiers": [{"type": "ISBN_10", "identifier": "966"}, {"type": "ISBN_13", "identifier": "978966"}]}},
                  {"id": "g2", "volumeInfo": {
                    "title": "Kobzar 2", "industryIdentifiers": [{"type": "ISBN_10", "identifier": "123"}]}},
                  {"id": "g3", "volumeInfo": {"title": "No ids", "categories": [],
                    "industryIdentifiers": [{"type": "OTHER", "identifier": "x"}]}},
                  {"id": "g4"}
                ]}
                """);

        List<BookCatalogSearchResultResponse> results = client.search("kobzar");

        server.verify();
        assertThat(results).hasSize(3);
        BookCatalogSearchResultResponse first = results.get(0);
        assertThat(first.title()).isEqualTo("Kobzar");
        assertThat(first.author()).isEqualTo("Taras Shevchenko, Editor");
        assertThat(first.genre()).isEqualTo("Poetry");
        assertThat(first.coverUrl()).isEqualTo("https://img/1.jpg");
        assertThat(first.isbn()).isEqualTo("978966");
        assertThat(first.externalId()).isEqualTo("g1");
        BookCatalogSearchResultResponse second = results.get(1);
        assertThat(second.isbn()).isEqualTo("123");
        assertThat(second.author()).isNull();
        assertThat(second.genre()).isNull();
        assertThat(second.coverUrl()).isNull();
        assertThat(results.get(2).isbn()).isNull();
        assertThat(results.get(2).genre()).isNull();
    }

    @Test
    @DisplayName("без результатів повертається порожній список")
    void noItems() {
        respond("{}");
        assertThat(client.search("kobzar")).isEmpty();
    }
}
