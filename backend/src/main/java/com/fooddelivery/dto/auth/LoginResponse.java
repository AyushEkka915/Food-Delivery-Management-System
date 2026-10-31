package com.fooddelivery.dto.auth;

public record LoginResponse(String accessToken, String tokenType, long expiresInSeconds) { }
