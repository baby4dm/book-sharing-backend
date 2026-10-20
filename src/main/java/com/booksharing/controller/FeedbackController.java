package com.booksharing.controller;

import com.booksharing.dto.req.CreateFeedbackRequest;
import com.booksharing.dto.req.UpdateFeedbackRequest;
import com.booksharing.dto.res.FeedbackListResponse;
import com.booksharing.dto.res.FeedbackResponse;
import com.booksharing.entity.User;
import com.booksharing.enums.FeedbackStatus;
import com.booksharing.enums.FeedbackTopic;
import com.booksharing.service.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse create(
            @Valid @RequestBody CreateFeedbackRequest request,
            @AuthenticationPrincipal(errorOnInvalidType = false) User currentUser,
            HttpServletRequest httpRequest) {
        return feedbackService.create(request, currentUser, httpRequest.getRemoteAddr());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public FeedbackListResponse search(
            @RequestParam(required = false) FeedbackStatus status,
            @RequestParam(required = false) FeedbackTopic topic,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return feedbackService.search(status, topic, page, size);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
    public FeedbackResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFeedbackRequest request,
            @AuthenticationPrincipal User currentUser) {
        return feedbackService.update(id, request, currentUser);
    }
}