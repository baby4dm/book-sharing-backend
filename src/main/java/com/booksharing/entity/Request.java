package com.booksharing.entity;

import com.booksharing.enums.DeliveryMethod;
import com.booksharing.enums.RequestStatus;
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
 * Заявка на книгу. Кілька заявок на один {@link Listing} утворюють чергу:
 * усі статуси {@code PENDING} видно власнику одночасно, і подавати нові
 * заявки можна навіть якщо одна вже {@code ACTIVE} (див. розділ "Заявка →
 * Черга → Обмін" у флоу сервісу). Мапиться на таблицю {@code requests}
 * (01-init-db.sql).
 */
@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @Column(name = "desired_deadline", nullable = false)
    private LocalDate desiredDeadline;

    // конкретний спосіб доставки, обраний читачем із тих, що підтримує
    // Listing (там може бути кілька); саме це значення перейде в
    // Exchange.deliveryMethod при підтвердженні заявки
    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "preferred_delivery_method", nullable = false, columnDefinition = "delivery_method")
    private DeliveryMethod preferredDeliveryMethod;

    @Column(columnDefinition = "text")
    private String message;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "request_status")
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "reject_comment", columnDefinition = "text")
    private String rejectComment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // не @UpdateTimestamp: decided_at виставляється явно сервісом лише в
    // момент підтвердження/відхилення, а не при кожному оновленні рядка
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
}