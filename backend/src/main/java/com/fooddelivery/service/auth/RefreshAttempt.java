package com.fooddelivery.service.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;

/** Internal lifecycle result; raw replacement credentials are never serialized or logged. */
public record RefreshAttempt(RefreshOutcome outcome, @JsonIgnore LoginSession session) {
    @Override
    public String toString() {
        return "RefreshAttempt[outcome=" + outcome + ", session=[REDACTED]]";
    }
}
