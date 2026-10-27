package com.booksharing.dto.res;

public record ModerationSummaryResponse(
        long openDisputes,
        long newFeedback,
        long restrictedUsers,
        long hiddenListings) {
}
