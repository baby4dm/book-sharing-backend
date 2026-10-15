package com.booksharing.controller;

import com.booksharing.dto.req.RejectRequestRequest;
import com.booksharing.dto.res.RequestListResponse;
import com.booksharing.dto.res.RequestResponse;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.RequestSort;
import com.booksharing.enums.RequestStatus;
import com.booksharing.service.RequestService;
import com.booksharing.entity.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @GetMapping("/my")
    public RequestListResponse getMyRequests(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) DeliveryMethod deliveryMethod,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(defaultValue = "NEWEST") RequestSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return requestService.getMyRequests(
                currentUser.getId(), q, deliveryMethod, status, sort, page, size);
    }

    @GetMapping("/received")
    public RequestListResponse getReceivedRequests(
            @AuthenticationPrincipal User currentUser,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) DeliveryMethod deliveryMethod,
            @RequestParam(required = false) RequestStatus status,
            @RequestParam(defaultValue = "NEWEST") RequestSort sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return requestService.getReceivedRequests(
                currentUser.getId(), q, deliveryMethod, status, sort, page, size);
    }

    @PatchMapping("/{id}/approve")
    public RequestResponse approve(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return requestService.approve(id, currentUser.getId());
    }

    @PatchMapping("/{id}/reject")
    public RequestResponse reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RejectRequestRequest request) {
        return requestService.reject(id, currentUser.getId(), request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        requestService.cancel(id, currentUser.getId());
    }
}