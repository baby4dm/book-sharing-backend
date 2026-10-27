package com.booksharing.dto.res;

import com.booksharing.mapper.ListingMapper;
import com.booksharing.enums.ListingStatus;
import com.booksharing.enums.SettlementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ListingResponse(
        UUID id,
        UUID ownerId,
        String ownerName,
        String ownerAvatarUrl,
        SettlementType settlementType,
        String region,
        String settlementName,
        BigDecimal ownerRatingAvg,
        UUID bookCatalogEntryId,
        String bookTitle,
        String bookAuthor,
        String bookCoverUrl,
        String bookGenre,
        String conditionDescription,
        List<String> deliveryMethods,
        ListingStatus status,
        List<String> photoUrls,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // != null — оголошення приховане модератором (бачать лише власник і модератори)
        LocalDateTime hiddenAt,
        String hiddenReason) {
}