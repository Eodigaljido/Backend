package com.eodigaljido.backend.exception;

import org.springframework.http.HttpStatus;

public class FollowingNewsException extends RuntimeException {

    private final HttpStatus status;

    public FollowingNewsException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
