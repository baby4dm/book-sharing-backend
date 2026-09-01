package com.booksharing.model.exchange;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Подається отримувачем посилки (не відправником) - див. флоу в
 * {@link ExchangeService#createShipment}. {@code direction} визначає,
 * хто саме отримувач: {@code TO_READER} - читач вказує свої дані,
 * {@code TO_OWNER} - власник (при поверненні).
 */
public record CreateShipmentRequest(
        @NotNull(message = "Потрібно вказати напрямок відправлення")
        ShipmentDirection direction,

        @NotBlank(message = "ПІБ отримувача обов'язкове")
        @Size(max = 255)
        String recipientName,

        @NotBlank(message = "Телефон отримувача обов'язковий")
        @Size(max = 32)
        String recipientPhone,

        @NotNull(message = "Потрібно вказати перевізника")
        ShipmentCarrier carrier,

        @NotBlank(message = "Місто обов'язкове")
        @Size(max = 255)
        String city,

        @NotBlank(message = "Номер відділення обов'язковий")
        @Size(max = 20)
        String branchNumber) {
}