package com.fooddelivery.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends CreatedOnlyEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64, columnDefinition = "CHAR(64)", updatable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    protected RefreshToken() { }

    public static RefreshToken create(User user, String tokenHash, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.user = Objects.requireNonNull(user, "user is required");
        token.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash is required");
        token.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt is required");
        token.revoked = false;
        return token;
    }

    public User getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }

    public void revoke() { revoked = true; }
}
