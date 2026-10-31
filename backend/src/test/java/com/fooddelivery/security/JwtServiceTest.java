package com.fooddelivery.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fooddelivery.config.JwtProperties;
import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-02T03:04:05Z");
    private final byte[] keyBytes = new byte[32];
    private JwtService jwtService;
    private AuthenticatedUser authenticatedUser;

    @BeforeEach
    void setUp() {
        for (int index = 0; index < keyBytes.length; index++) {
            keyBytes[index] = (byte) (index + 1);
        }
        jwtService = new JwtService(properties(keyBytes), Clock.fixed(NOW, ZoneOffset.UTC));

        User user = mock(User.class);
        Role role = mock(Role.class);
        when(user.getId()).thenReturn(37L);
        when(user.getEmail()).thenReturn("  USER@Example.com ");
        when(user.getPasswordHashForAuthentication()).thenReturn("unused-test-hash");
        when(role.getName()).thenReturn("CUSTOMER");
        when(user.getRoles()).thenReturn(Set.of(role));
        authenticatedUser = AuthenticatedUser.from(user);
    }

    @Test
    void generatesHs256TokenWithTrustedIdentityClaimsAndFifteenMinuteExpiry() {
        String token = jwtService.generateAccessToken(authenticatedUser);
        Claims claims = parse(token, keyBytes);

        assertEquals("37", claims.getSubject());
        assertEquals("user@example.com", claims.get("email", String.class));
        assertEquals(java.util.List.of("ROLE_CUSTOMER"), claims.get("authorities", java.util.List.class));
        assertEquals(Date.from(NOW), claims.getIssuedAt());
        assertEquals(Date.from(NOW.plusSeconds(900)), claims.getExpiration());
        assertNotEquals(null, claims.getId());
    }

    @Test
    void validatesTokenAndReturnsDatabaseIdentityAndAuthorities() {
        AuthenticatedUser principal = jwtService.validateAccessToken(jwtService.generateAccessToken(authenticatedUser));

        assertEquals(37L, principal.getUserId());
        assertEquals("user@example.com", principal.getEmail());
        assertEquals("ROLE_CUSTOMER", principal.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void rejectsExpiredToken() {
        String token = Jwts.builder()
                .subject("37")
                .claim("email", "user@example.com")
                .claim("authorities", java.util.List.of("ROLE_CUSTOMER"))
                .issuedAt(Date.from(NOW.minusSeconds(1200)))
                .expiration(Date.from(NOW.minusSeconds(300)))
                .id("expired-id")
                .signWith(Keys.hmacShaKeyFor(keyBytes), Jwts.SIG.HS256)
                .compact();

        assertThrows(InvalidAccessTokenException.class, () -> jwtService.validateAccessToken(token));
    }

    @Test
    void rejectsIncorrectSignature() {
        byte[] otherKey = keyBytes.clone();
        otherKey[0] ^= 0x5A;
        String token = new JwtService(properties(otherKey), Clock.fixed(NOW, ZoneOffset.UTC))
                .generateAccessToken(authenticatedUser);

        assertThrows(InvalidAccessTokenException.class, () -> jwtService.validateAccessToken(token));
    }

    @Test
    void rejectsMalformedToken() {
        assertThrows(InvalidAccessTokenException.class, () -> jwtService.validateAccessToken("not.a.jwt"));
    }

    @Test
    void rejectsMissingAndWeakSecretsAtInitialization() {
        assertThrows(IllegalStateException.class, () -> new JwtProperties("", 15, 7));
        String weakKey = Base64.getEncoder().encodeToString(new byte[24]);
        assertThrows(IllegalStateException.class,
                () -> new JwtService(new JwtProperties(weakKey, 15, 7), Clock.systemUTC()));
    }

    private Claims parse(String token, byte[] key) {
        SecretKey signingKey = Keys.hmacShaKeyFor(key);
        return Jwts.parser().verifyWith(signingKey).clock(() -> Date.from(NOW)).build()
                .parseSignedClaims(token).getPayload();
    }

    private static JwtProperties properties(byte[] key) {
        return new JwtProperties(Base64.getEncoder().encodeToString(key), 15, 7);
    }
}
