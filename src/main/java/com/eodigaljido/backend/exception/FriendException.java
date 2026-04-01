package com.eodigaljido.backend.exception;

import org.springframework.http.HttpStatus;

public class FriendException extends RuntimeException {

    private final HttpStatus status;

    public FriendException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
