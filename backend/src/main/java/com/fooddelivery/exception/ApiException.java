package com.fooddelivery.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for all application exceptions. Carries an HTTP status and a stable
 * error code so the frontend can react without parsing English messages.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
