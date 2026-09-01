package com.booksharing.model.request;

import com.booksharing.model.user.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listings/{listingId}/requests")
@RequiredArgsConstructor
public class ListingRequestController {

    private final RequestService requestService;

    @GetMapping
    public List<RequestResponse> getQueue(@PathVariable UUID listingId) {
        return requestService.getQueueForListing(listingId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponse submit(
            @PathVariable UUID listingId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateRequestRequest request) {
        return requestService.submit(listingId, currentUser.getId(), request);
    }
}