package com.booksharing.controller;

import com.booksharing.dto.req.CreateReviewRequest;
import com.booksharing.dto.res.ExchangeResponse;
import com.booksharing.entity.User;
import com.booksharing.service.ReviewService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/exchanges/{exchangeId}/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExchangeResponse create(
            @PathVariable UUID exchangeId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateReviewRequest request) {
        return reviewService.create(exchangeId, currentUser.getId(), request);
    }
}