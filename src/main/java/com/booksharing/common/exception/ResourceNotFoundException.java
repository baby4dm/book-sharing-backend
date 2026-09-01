package com.booksharing.common.exception;

/**
 * Кидається, коли сутність за id не знайдено. {@link GlobalExceptionHandler}
 * перетворює її на HTTP 404 з акуратним JSON-тілом, а не на голий 500.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}