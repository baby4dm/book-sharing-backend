package com.booksharing.controller;

import com.booksharing.dto.req.RejectRequestRequest;
import com.booksharing.dto.res.RequestResponse;
import com.booksharing.service.RequestService;
import com.booksharing.entity.User;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @GetMapping("/my")
    public List<RequestResponse> getMyRequests(@AuthenticationPrincipal User currentUser) {
        return requestService.getMyRequests(currentUser.getId());
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