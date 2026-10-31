package com.fooddelivery.dto.common;

/**
 * Uniform success envelope for all API responses.
 */
public record ApiResponse<T>(boolean success, String message, T data, java.time.Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "OK", data, java.time.Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, java.time.Instant.now());
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, message, null, java.time.Instant.now());
    }
}
