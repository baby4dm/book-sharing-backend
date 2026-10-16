package com.booksharing.dto.res;

import com.booksharing.enums.ExchangeStatus;
import java.util.List;
import java.util.Map;

public record ExchangeListResponse(
        List<ExchangeResponse> items,
        Map<ExchangeStatus, Long> statusCounts,
        long total,
        int page,
        int totalPages,
        long totalElements
) {
}