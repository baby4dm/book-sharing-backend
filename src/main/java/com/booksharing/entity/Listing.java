package com.booksharing.entity;

import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.ListingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

/**
 * Оголошення про конкретний примірник книги (не сама книга — довідкові дані
 * про книгу описує {@link BookCatalogEntry}). Мапиться на таблицю
 * {@code listings} (01-init-db.sql).
 * <p>
 * {@code deliveryMethods} зберігається як Postgres {@code text[]} з
 * CHECK-обмеженням у БД (а не нативний масив enum — див. коментар у
 * {@link DeliveryMethod}). Валідність значень (тільки "PICKUP"/"MAIL")
 * контролюється на рівні сервісу через enum {@link DeliveryMethod}.
 */
@Entity
@Table(name = "listings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_catalog_entry_id", nullable = false)
    private BookCatalogEntry bookCatalogEntry;

    @Column(name = "condition_description", columnDefinition = "text")
    private String conditionDescription;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "delivery_methods", columnDefinition = "text[]", nullable = false)
    @Builder.Default
    private List<String> deliveryMethods = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "listing_status")
    @Builder.Default
    private ListingStatus status = ListingStatus.AVAILABLE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}