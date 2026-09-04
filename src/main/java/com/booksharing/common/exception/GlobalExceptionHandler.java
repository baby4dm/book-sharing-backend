package com.booksharing.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Наскрізна обробка помилок для всіх контролерів. Кожен новий модуль
 * (Listing, Request, Exchange...) не пише власний try/catch —
 * достатньо кидати {@link ResourceNotFoundException} чи покладатися на
 * {@code @Valid}, а тіло відповіді й статус-код формуються тут один раз.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(ResourceNotFoundException ex) {
        return ErrorResponse.of(ex.getMessage(), HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ValidationErrorResponse handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError -> fieldErrors.put(
                        fieldError.getField(), fieldError.getDefaultMessage()));
        return ValidationErrorResponse.of(fieldErrors);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleIllegalArgument(IllegalArgumentException ex) {
        return ErrorResponse.of(ex.getMessage(), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDenied(AccessDeniedException ex) {
        return ErrorResponse.of("Немає доступу до цієї дії", HttpStatus.FORBIDDEN.value());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidCredentials(InvalidCredentialsException ex) {
        return ErrorResponse.of(ex.getMessage(), HttpStatus.UNAUTHORIZED.value());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleIllegalState(IllegalStateException ex) {
        return ErrorResponse.of(ex.getMessage(), HttpStatus.CONFLICT.value());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneric(Exception ex) {
        return ErrorResponse.of("Внутрішня помилка сервера", HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}