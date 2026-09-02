package com.booksharing.controller;

import com.booksharing.dto.res.BookCatalogEntryResponse;
import com.booksharing.dto.res.BookCatalogSearchResultResponse;
import com.booksharing.service.BookCatalogService;
import com.booksharing.dto.req.ResolveBookCatalogEntryRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/book-catalog")
@RequiredArgsConstructor
public class BookCatalogController {

    private final BookCatalogService bookCatalogService;

    @GetMapping("/search")
    public List<BookCatalogSearchResultResponse> search(@RequestParam String q) {
        return bookCatalogService.search(q);
    }

    @PostMapping("/resolve")
    public BookCatalogEntryResponse resolve(@Valid @RequestBody ResolveBookCatalogEntryRequest request) {
        return bookCatalogService.resolve(request);
    }

    @GetMapping("/{id}")
    public BookCatalogEntryResponse getById(@PathVariable UUID id) {
        return bookCatalogService.getById(id);
    }
}