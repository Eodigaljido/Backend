package com.eodigaljido.backend.exception;

import org.springframework.http.HttpStatus;

public class RouteException extends RuntimeException {

    private final HttpStatus status;

    public RouteException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
