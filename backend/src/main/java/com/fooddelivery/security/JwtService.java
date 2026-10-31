package com.fooddelivery.security;

import com.fooddelivery.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    public static final Duration ACCESS_TOKEN_LIFETIME = Duration.ofMinutes(15);
    private static final String EMAIL_CLAIM = "email";
    private static final String AUTHORITIES_CLAIM = "authorities";

    private final SecretKey signingKey;
    private final Clock clock;

    public JwtService(JwtProperties properties, Clock clock) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(properties.secret());
            if (keyBytes.length < 32) {
                throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes for HS256");
            }
            this.signingKey = Keys.hmacShaKeyFor(keyBytes);
            this.clock = clock;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET must be valid Base64 containing at least 32 bytes", exception);
        }
    }

    public String generateAccessToken(AuthenticatedUser user) {
        Instant issuedAt = clock.instant();
        List<String> authorities = user.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .sorted()
                .toList();

        return Jwts.builder()
                .header().type("JWT").and()
                .subject(user.getUserId().toString())
                .claim(EMAIL_CLAIM, user.getEmail())
                .claim(AUTHORITIES_CLAIM, authorities)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(ACCESS_TOKEN_LIFETIME)))
                .id(UUID.randomUUID().toString())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public AuthenticatedUser validateAccessToken(String compactToken) {
        try {
            Jws<Claims> parsed = Jwts.parser()
                    .verifyWith(signingKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(compactToken);

            if (!"HS256".equals(parsed.getHeader().getAlgorithm())) {
                throw new InvalidAccessTokenException();
            }
            return principalFromClaims(parsed.getPayload(), clock.instant());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidAccessTokenException();
        }
    }

    private static AuthenticatedUser principalFromClaims(Claims claims, Instant now) {
        String subject = claims.getSubject();
        String email = claims.get(EMAIL_CLAIM, String.class);
        String tokenId = claims.getId();
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();
        Object rawAuthorities = claims.get(AUTHORITIES_CLAIM);

        if (subject == null || email == null || email.isBlank() || tokenId == null || tokenId.isBlank()
                || issuedAt == null || expiration == null || !(rawAuthorities instanceof List<?> values)) {
            throw new InvalidAccessTokenException();
        }
        Instant issuedInstant = issuedAt.toInstant();
        Instant expirationInstant = expiration.toInstant();
        if (issuedInstant.isAfter(now)
                || !expirationInstant.isAfter(issuedInstant)
                || Duration.between(issuedInstant, expirationInstant).compareTo(ACCESS_TOKEN_LIFETIME) > 0) {
            throw new InvalidAccessTokenException();
        }

        final long userId;
        try {
            userId = Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new InvalidAccessTokenException();
        }
        if (userId <= 0) {
            throw new InvalidAccessTokenException();
        }

        Set<String> authorities = values.stream()
                .map(value -> {
                    if (!(value instanceof String authority)
                            || !authority.startsWith("ROLE_")
                            || authority.length() <= "ROLE_".length()) {
                        throw new InvalidAccessTokenException();
                    }
                    return authority;
                })
                .collect(Collectors.toUnmodifiableSet());
        return AuthenticatedUser.fromVerifiedJwt(userId, email, authorities);
    }
}
