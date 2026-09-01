package com.booksharing.model.listing;

import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Часткове оновлення оголошення власником. {@code status} тут навмисно
 * обмежений: власник вручну може перемкнути лише {@code AVAILABLE} ↔
 * {@code ARCHIVED}. Статуси {@code RESERVED}/{@code IN_EXCHANGE}
 * виставляються автоматично сервісами Request/Exchange, а не через цей
 * ендпоінт - {@link ListingService} відхилить спробу встановити їх тут.
 */
public record UpdateListingRequest(
        @Size(max = 2000) String conditionDescription,
        List<String> deliveryMethods,
        ListingStatus status) {
}