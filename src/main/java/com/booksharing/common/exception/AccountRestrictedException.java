package com.booksharing.common.exception;

public class AccountRestrictedException extends RuntimeException {

    public AccountRestrictedException(String message) {
        super(message);
    }
}
