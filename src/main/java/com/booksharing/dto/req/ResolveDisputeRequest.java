package com.booksharing.dto.req;

import com.booksharing.enums.DisputeStatus;
import com.booksharing.service.DisputeService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * {@code status} має бути одним із трьох термінальних значень
 * ({@code RESOLVED_FAVOR_FILER}/{@code RESOLVED_FAVOR_OTHER}/{@code DISMISSED})
 * - {@code OPEN} відхиляється в {@link DisputeService#resolve}.
 */
public record ResolveDisputeRequest(
        @NotNull(message = "Потрібно вказати рішення")
        DisputeStatus status,

        @NotBlank(message = "Потрібно обґрунтувати рішення")
        @Size(max = 4000)
        String resolutionComment) {
}