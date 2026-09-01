package com.booksharing.model.dispute;

import com.booksharing.model.user.User;
import jakarta.validation.Valid;
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
@RequiredArgsConstructor
public class ExchangeDisputeController {

    private final DisputeService disputeService;

    @PostMapping("/api/exchanges/{exchangeId}/disputes")
    @ResponseStatus(HttpStatus.CREATED)
    public DisputeResponse fileDispute(
            @PathVariable UUID exchangeId,
            @AuthenticationPrincipal User currentUser,
            @Valid @RequestBody CreateDisputeRequest request) {
        return disputeService.fileDispute(exchangeId, currentUser.getId(), request);
    }

    @GetMapping("/api/disputes/{id}")
    public DisputeResponse getById(@PathVariable UUID id, @AuthenticationPrincipal User currentUser) {
        return disputeService.getById(id, currentUser.getId());
    }
}