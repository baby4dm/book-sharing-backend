package com.booksharing.common.exception;

import java.time.Instant;
import java.util.Map;

public record ValidationErrorResponse(
        String message, Map<String, String> fieldErrors, int status, Instant timestamp) {

    public static ValidationErrorResponse of(Map<String, String> fieldErrors) {
        return new ValidationErrorResponse(
                "Помилка валідації вхідних даних", fieldErrors, 400, Instant.now());
    }
}