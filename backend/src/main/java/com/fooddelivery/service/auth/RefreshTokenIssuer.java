package com.fooddelivery.service.auth;

import com.fooddelivery.config.JwtProperties;
import com.fooddelivery.entity.RefreshToken;
import com.fooddelivery.entity.User;
import com.fooddelivery.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenIssuer {
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecureRandom secureRandom;

    public RefreshTokenIssuer(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties,
                              Clock clock, SecureRandom secureRandom) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    public String issueFor(User user) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                clock.instant().plus(Duration.ofDays(jwtProperties.refreshTtlDays())), ZoneOffset.UTC);

        refreshTokenRepository.save(RefreshToken.create(user, sha256Hex(rawToken), expiresAt));
        return rawToken;
    }

    private static String sha256Hex(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
