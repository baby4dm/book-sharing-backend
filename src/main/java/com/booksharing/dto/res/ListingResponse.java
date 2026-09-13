package com.booksharing.dto.res;

import com.booksharing.mapper.ListingMapper;
import com.booksharing.enums.ListingStatus;
import com.booksharing.enums.SettlementType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Сплющений DTO: власник і довідкові дані книги вкладені прямо в
 * оголошення, щоб фронтенд не робив додаткових запитів для стрічки.
 * {@code photoUrls} підвантажується сервісом окремо (у {@code Listing}
 * немає навігаційної колекції на {@code ListingPhoto} — див. коментар
 * у {@link ListingMapper}).
 * <p>
 * {@code settlementType}/{@code region}/{@code settlementName} - це
 * ЕФЕКТИВНА локація (власний override оголошення, якщо є, інакше -
 * населений пункт власника) - {@link ListingMapper} рахує це один раз,
 * фронтенду не потрібно знати про існування override взагалі.
 */
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
        LocalDateTime updatedAt) {
}