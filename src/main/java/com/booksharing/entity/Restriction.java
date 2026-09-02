package com.booksharing.entity;

import com.booksharing.enums.RestrictionType;
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
 * Обмеження облікового запису користувача (тимчасове чи повне),
 * зазвичай породжене рішенням модератора по {@link Dispute}, але
 * {@code dispute} може бути {@code null} — наприклад при простроченні
 * без окремої скарги.
 * Мапиться на таблицю {@code restrictions} (01-init-db.sql).
 */
@Entity
@Table(name = "restrictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Restriction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "restriction_type")
    private RestrictionType type;

    @Column(columnDefinition = "text")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id")
    private Dispute dispute;

    @CreationTimestamp
    @Column(name = "starts_at", nullable = false, updatable = false)
    private LocalDateTime startsAt;

    // null = безстрокове (для PERMANENT) або ще не визначено
    @Column(name = "ends_at")
    private LocalDateTime endsAt;
}