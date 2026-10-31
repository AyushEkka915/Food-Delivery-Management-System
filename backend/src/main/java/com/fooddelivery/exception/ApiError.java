package com.fooddelivery.exception;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error envelope returned by {@link GlobalExceptionHandler}.
 * Field-level validation errors are carried in {@code fieldErrors}.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }

    public static ApiError of(int status, String code, String message) {
        return new ApiError(Instant.now(), status, code, message, List.of());
    }

    public static ApiError of(int status, String code, String message, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, code, message, fieldErrors);
    }
}
