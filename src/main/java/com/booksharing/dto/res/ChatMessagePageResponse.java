package com.booksharing.dto.res;

import java.util.List;

public record ChatMessagePageResponse(List<ChatMessageResponse> items, boolean hasMore) {
}