package com.booksharing.entity;

import com.booksharing.enums.ShipmentCarrier;
import com.booksharing.enums.ShipmentDirection;
import com.booksharing.enums.ShipmentStatus;
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
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.type.PostgreSQLEnumJdbcType;

/**
 * Інформація про поштове відправлення (актуально лише коли
 * {@code Exchange.deliveryMethod == MAIL}). Один рядок = одна посилка в
 * одному напрямку ({@code direction}): при видачі книги отримувач
 * (читач) вказує свої контактні дані ще ДО відправки — рядок створюється
 * зі статусом {@code PENDING}; відправник (власник) вантажить фото
 * накладної й статус міняється на {@code SHIPPED}; отримувач підтверджує
 * отримання — {@code DELIVERED}. При поверненні книги напрямок і ролі
 * дзеркальні ({@code TO_OWNER}).
 * <p>
 * MVP-версія: контактні дані вводяться вручну, без інтеграції з API
 * поштових служб.
 * Мапиться на таблицю {@code shipment_info} (01-init-db.sql).
 */
@Entity
@Table(name = "shipment_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShipmentInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_id", nullable = false)
    private Exchange exchange;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "shipment_direction")
    private ShipmentDirection direction;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 32)
    private String recipientPhone;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "shipment_carrier")
    private ShipmentCarrier carrier;

    // місто зберігається окремо від номера відділення: номер відділення
    // не унікальний глобально (відділення №5 є в кожному місті)
    @Column(nullable = false)
    private String city;

    @Column(name = "branch_number", nullable = false, length = 20)
    private String branchNumber;

    @Column(name = "waybill_photo_url", length = 512)
    private String waybillPhotoUrl;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, columnDefinition = "shipment_status")
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.PENDING;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}