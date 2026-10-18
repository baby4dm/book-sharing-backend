package com.booksharing.dto.res;

import java.util.List;

public record UserReviewListResponse(
        List<UserReviewResponse> items,
        int page,
        int totalPages,
        long totalElements) {
}