package com.fooddelivery.service.auth;

import com.fooddelivery.dto.auth.LoginResponse;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** Internal login result. The refresh token is for the response cookie only, never JSON. */
public record LoginSession(LoginResponse accessTokenResponse, @JsonIgnore String refreshToken) {
    @Override
    public String toString() {
        return "LoginSession[accessTokenResponse=[REDACTED], refreshToken=[REDACTED]]";
    }
}
