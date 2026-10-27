package com.booksharing.dto.res;

import java.util.List;

public record ModerationUserDetailResponse(
        ModerationUserResponse user,
        List<RestrictionResponse> restrictions,
        List<ModerationActionResponse> actions,
        long disputesFiled,
        long disputesAgainst) {
}
