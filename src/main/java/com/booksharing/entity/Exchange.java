package com.booksharing.entity;

import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.ExchangeStatus;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
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
 * Обмін книгою — центральна сутність бізнес-логіки, породжується з
 * підтвердженої {@link Request} (звʼязок 1:1, request_id UNIQUE у БД).
 * {@code deliveryMethod} тут — скалярне значення (на відміну від
 * {@code Listing.deliveryMethods}, де це масив), тому мапиться напряму
 * як нативний Postgres enum без обхідних рішень.
 * Мапиться на таблицю {@code exchanges} (01-init-db.sql).
 */
@Entity
@Table(name = "exchanges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false, unique = true)
    private Request request;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reader_id", nullable = false)
    private User reader;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "delivery_method", nullable = false, columnDefinition = "delivery_method")
    private DeliveryMethod deliveryMethod;

    @Column(nullable = false)
    private LocalDate deadline;

    @Column(name = "extended_deadline")
    private LocalDate extendedDeadline;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "exchange_status")
    @Builder.Default
    private ExchangeStatus status = ExchangeStatus.HANDOVER_PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // не автоматичний timestamp: completed_at виставляє сервіс лише в момент
    // фактичного завершення обміну (підтвердження повернення власником)
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}