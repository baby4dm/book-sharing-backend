package com.booksharing.common.exception;

/**
 * Кидається при невдалій спробі логіну email+пароль (невірний пароль,
 * акаунт зареєстрований лише через Google, чи обліковий запис
 * заблокований). {@link GlobalExceptionHandler} перетворює її на 401.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}