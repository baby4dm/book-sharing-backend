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

    @Column(name = "reading_days", nullable = false)
    private Integer readingDays;

    @Column
    private LocalDate deadline;

    @Column(name = "extended_deadline")
    private LocalDate extendedDeadline;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "exchange_status")
    @Builder.Default
    private ExchangeStatus status = ExchangeStatus.HANDOVER_PENDING;

    @Column(name = "dispute_reason")
    private String disputeReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "reading_started_at")
    private LocalDateTime readingStartedAt;

    @Column(name = "return_prepared_at")
    private LocalDateTime returnPreparedAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "reading_paused_days", nullable = false)
    @Builder.Default
    private Integer readingPausedDays = 0;

    public LocalDate getDueDate() {
        LocalDate base = extendedDeadline != null ? extendedDeadline : deadline;
        if (base == null) {
            return null;
        }
        return base.plusDays(readingPausedDays == null ? 0 : readingPausedDays);
    }
}