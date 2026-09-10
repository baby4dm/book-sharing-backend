package com.booksharing.controller;

import com.booksharing.dto.req.AddListingPhotoRequest;
import com.booksharing.dto.req.CreateListingRequest;
import com.booksharing.dto.res.ListingResponse;
import com.booksharing.service.ListingService;
import com.booksharing.enums.ListingStatus;
import com.booksharing.dto.req.UpdateListingRequest;
import com.booksharing.entity.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingService listingService;

    @GetMapping
    public Page<ListingResponse> search(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String deliveryMethod,
            @RequestParam(required = false) ListingStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 12) Pageable pageable) {
        return listingService.search(genre, city, deliveryMethod, status, search, pageable);
    }

    @GetMapping("/{id}")
    public ListingResponse getById(@PathVariable UUID id) {
        return listingService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ListingResponse create(
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateListingRequest request) {
        return listingService.create(currentUser.getId(), request);
    }

    @PatchMapping("/{id}")
    public ListingResponse update(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody UpdateListingRequest request) {
        return listingService.update(id, currentUser.getId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        listingService.delete(id, currentUser.getId());
    }

    @PostMapping("/{id}/photos")
    public ListingResponse addPhoto(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody AddListingPhotoRequest request) {
        return listingService.addPhoto(id, currentUser.getId(), request);
    }
}