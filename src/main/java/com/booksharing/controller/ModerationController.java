package com.booksharing.controller;

import com.booksharing.dto.req.LiftRestrictionRequest;
import com.booksharing.dto.req.ModerationReasonRequest;
import com.booksharing.dto.req.ResolveDisputeRequest;
import com.booksharing.dto.req.RestrictUserRequest;
import com.booksharing.dto.res.DisputeResponse;
import com.booksharing.dto.res.ListingResponse;
import com.booksharing.dto.res.ModerationActionResponse;
import com.booksharing.dto.res.ModerationDisputeResponse;
import com.booksharing.dto.res.ModerationSummaryResponse;
import com.booksharing.dto.res.ModerationUserDetailResponse;
import com.booksharing.dto.res.ModerationUserResponse;
import com.booksharing.entity.User;
import com.booksharing.enums.UserStatus;
import com.booksharing.service.DisputeService;
import com.booksharing.service.ModerationService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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

/**
 * Панель модератора. Увесь контролер доступний лише {@code MODERATOR}/{@code ADMIN}
 * ({@code @PreAuthorize} на класі); рішення по скарзі додатково захищене
 * {@code @PreAuthorize} у {@link DisputeService}.
 */
@RestController
@RequestMapping("/api/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
public class ModerationController {

    private final DisputeService disputeService;
    private final ModerationService moderationService;

    @GetMapping("/summary")
    public ModerationSummaryResponse summary() {
        return moderationService.summary();
    }

    // ---- скарги

    /** {@code status=OPEN} (за замовчуванням) — відкриті, {@code RESOLVED} — останні розглянуті. */
    @GetMapping("/disputes")
    public List<ModerationDisputeResponse> listDisputes(
            @RequestParam(defaultValue = "OPEN") String status) {
        return moderationService.listDisputes(!"RESOLVED".equalsIgnoreCase(status));
    }

    @GetMapping("/disputes/{id}")
    public ModerationDisputeResponse getDispute(@PathVariable UUID id) {
        return moderationService.getDispute(id);
    }

    @PatchMapping("/disputes/{id}/resolve")
    public DisputeResponse resolve(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ResolveDisputeRequest request) {
        return disputeService.resolve(id, currentUser.getId(), request);
    }

    // ---- користувачі

    @GetMapping("/users")
    public Page<ModerationUserResponse> searchUsers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return moderationService.searchUsers(q, status, page, size);
    }

    @GetMapping("/users/{id}")
    public ModerationUserDetailResponse getUser(@PathVariable UUID id) {
        return moderationService.getUser(id);
    }

    @PostMapping("/users/{id}/restrict")
    public ModerationUserDetailResponse restrictUser(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody RestrictUserRequest request) {
        return moderationService.restrictUser(currentUser.getId(), id, request);
    }

    @PostMapping("/users/{id}/lift")
    public ModerationUserDetailResponse liftRestriction(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody(required = false) LiftRestrictionRequest request) {
        return moderationService.liftRestriction(currentUser.getId(), id, request);
    }

    // ---- оголошення

    @GetMapping("/listings/hidden")
    public Page<ListingResponse> hiddenListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return moderationService.hiddenListings(page, size);
    }

    @PostMapping("/listings/{id}/hide")
    public ListingResponse hideListing(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ModerationReasonRequest request) {
        return moderationService.hideListing(currentUser.getId(), id, request);
    }

    @PostMapping("/listings/{id}/restore")
    public ListingResponse restoreListing(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return moderationService.restoreListing(currentUser.getId(), id);
    }

    // ---- пости й коментарі (тіло з причиною, тому POST, а не DELETE)

    @PostMapping("/posts/{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePost(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ModerationReasonRequest request) {
        moderationService.deletePost(currentUser.getId(), id, request);
    }

    @PostMapping("/comments/{id}/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ModerationReasonRequest request) {
        moderationService.deleteComment(currentUser.getId(), id, request);
    }

    // ---- журнал

    /** {@code scope=content} — лише оголошення, пости й коментарі; інакше всі дії. */
    @GetMapping("/actions")
    public Page<ModerationActionResponse> actions(
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return moderationService.actions("content".equalsIgnoreCase(scope), page, size);
    }
}
