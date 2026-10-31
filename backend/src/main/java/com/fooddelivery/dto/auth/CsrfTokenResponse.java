package com.fooddelivery.dto.auth;

/** CSRF token and the header name a browser client must echo on unsafe requests. */
public record CsrfTokenResponse(String token, String headerName) {
}
