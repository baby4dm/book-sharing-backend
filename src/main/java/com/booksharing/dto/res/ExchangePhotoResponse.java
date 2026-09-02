package com.booksharing.dto.res;

import com.booksharing.enums.PhotoStage;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExchangePhotoResponse(
        UUID id,
        UUID uploadedByUserId,
        String uploadedByName,
        PhotoStage stage,
        String url,
        String note,
        LocalDateTime createdAt) {
}