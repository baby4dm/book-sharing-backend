package com.booksharing.dto.res;
import com.booksharing.enums.RequestStatus;

import java.util.List;
import java.util.Map;

public record RequestListResponse(
        List<RequestResponse> items,
        Map<RequestStatus, Long> statusCounts,
        long total,
        int page,
        int totalPages,
        long totalElements
) {
}