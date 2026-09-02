package com.booksharing.controller;

import com.booksharing.dto.res.DisputeResponse;
import com.booksharing.dto.req.ResolveDisputeRequest;
import com.booksharing.entity.User;
import com.booksharing.service.DisputeService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Авторизація {@code MODERATOR}/{@code ADMIN} перевіряється на рівні
 * методів сервісу ({@code @PreAuthorize}) у {@link DisputeService}, а не
 * тут - так само спрацює й при прямому виклику сервісу з тестів.
 */
@RestController
@RequestMapping("/api/moderation/disputes")
@RequiredArgsConstructor
public class ModerationController {

    private final DisputeService disputeService;

    @GetMapping
    public List<DisputeResponse> listOpenDisputes() {
        return disputeService.listOpenDisputes();
    }

    @PatchMapping("/{id}/resolve")
    public DisputeResponse resolve(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody ResolveDisputeRequest request) {
        return disputeService.resolve(id, currentUser.getId(), request);
    }
}