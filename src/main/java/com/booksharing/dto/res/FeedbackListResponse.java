package com.booksharing.dto.res;

import com.booksharing.enums.FeedbackStatus;
import java.util.List;
import java.util.Map;

public record FeedbackListResponse(
        List<FeedbackResponse> items,
        Map<FeedbackStatus, Long> statusCounts,
        int page,
        int totalPages,
        long totalElements) {
}