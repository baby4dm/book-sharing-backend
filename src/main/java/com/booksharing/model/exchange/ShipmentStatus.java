package com.booksharing.model.exchange;

/**
 * Відповідає Postgres-типу {@code shipment_status}.
 * {@code PENDING} — отримувач уже вказав контактні дані для доставки,
 * але відправник ще не завантажив фото накладної.
 */
public enum ShipmentStatus {
    PENDING,
    SHIPPED,
    DELIVERED
}