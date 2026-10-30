package com.booksharing.support;

import com.booksharing.entity.BookCatalogEntry;
import com.booksharing.entity.Exchange;
import com.booksharing.entity.Listing;
import com.booksharing.entity.Request;
import com.booksharing.entity.User;
import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.ExchangeStatus;
import com.booksharing.enums.ListingStatus;
import com.booksharing.enums.RequestStatus;
import com.booksharing.enums.UserRole;
import com.booksharing.enums.UserStatus;
import com.booksharing.service.UserStatsService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Фабрика тестових сутностей. Кожен виклик створює новий об'єкт з випадковим id,
 * тому тести не залежать один від одного.
 */
public final class TestData {

    private TestData() {
    }

    public static User user(String name) {
        return user(name, UserRole.USER);
    }

    public static User user(String name, UserRole role) {
        return User.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(name.toLowerCase().replace(' ', '.') + "@test.ua")
                .avatarUrl("/uploads/" + name + ".jpg")
                .role(role)
                .status(UserStatus.ACTIVE)
                .ratingAvg(BigDecimal.valueOf(4.5))
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .build();
    }

    public static BookCatalogEntry book(String title) {
        return BookCatalogEntry.builder()
                .id(UUID.randomUUID())
                .title(title)
                .author("Леся Українка")
                .genre("Поезія")
                .coverUrl("/covers/" + title + ".jpg")
                .build();
    }

    public static Listing listing(User owner) {
        return Listing.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .bookCatalogEntry(book("Лісова пісня"))
                .conditionDescription("Гарний стан")
                .deliveryMethods(new ArrayList<>(List.of("PICKUP", "MAIL")))
                .status(ListingStatus.AVAILABLE)
                .build();
    }

    public static Request request(Listing listing, User requester, RequestStatus status) {
        return Request.builder()
                .id(UUID.randomUUID())
                .listing(listing)
                .requester(requester)
                .readingDays(14)
                .preferredDeliveryMethod(DeliveryMethod.PICKUP)
                .message("Дуже хочу прочитати")
                .status(status)
                .createdAt(LocalDateTime.of(2026, 9, 1, 12, 0))
                .build();
    }

    public static Exchange exchange(User owner, User reader, DeliveryMethod method, ExchangeStatus status) {
        return Exchange.builder()
                .id(UUID.randomUUID())
                .owner(owner)
                .reader(reader)
                .listing(listing(owner))
                .deliveryMethod(method)
                .readingDays(14)
                .status(status)
                .createdAt(LocalDateTime.of(2026, 9, 1, 12, 0))
                .build();
    }

    public static UserStatsService.UserExchangeStats noStats() {
        return new UserStatsService.UserExchangeStats(0, 0, 0, 0, 0);
    }
}
