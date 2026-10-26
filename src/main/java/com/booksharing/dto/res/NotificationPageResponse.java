package com.booksharing.dto.res;


import java.util.List;

public record NotificationPageResponse(List<NotificationResponse> items, boolean hasMore) {
}