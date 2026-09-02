package com.booksharing.entity;

import com.booksharing.enums.BookSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

/**
 * Довідкові дані про книгу (назва, автор, ISBN, обкладинка) —
 * кеш зовнішнього API (Google Books) або ручний ввід.
 * Мапиться на таблицю {@code book_catalog_entries}
 * <p>
 * Конкретний примірник, який хтось виставляє на обмін, описує
 * окрема сутність {@code Listing}, що посилається на цей запис.
 */
@Entity
@Table(name = "book_catalog_entries")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(length = 20, unique = true)
    private String isbn;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(length = 500)
    private String author;

    @Column(columnDefinition = "text")
    private String description;

    private String genre;

    @Column(name = "cover_url", length = 512)
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "book_source")
    @Builder.Default
    private BookSource source = BookSource.MANUAL;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}