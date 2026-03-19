package com.eodigaljido.backend.exception;

import org.springframework.http.HttpStatus;

public class PhoneVerificationException extends RuntimeException {

    private final HttpStatus status;

    public PhoneVerificationException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
