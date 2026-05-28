package com.eodigaljido.backend.exception;

import org.springframework.http.HttpStatus;

public class CourseScheduleException extends RuntimeException {

    private final HttpStatus status;

    public CourseScheduleException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
