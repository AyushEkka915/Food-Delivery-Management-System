package com.fooddelivery.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fooddelivery.config.JwtProperties;
import com.fooddelivery.entity.RefreshToken;
import com.fooddelivery.entity.User;
import com.fooddelivery.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshTokenIssuerTest {
    private static final Instant NOW = Instant.parse("2026-06-01T10:15:30Z");

    @Test
    void generatesDistinctBase64UrlTokensAndPersistsOnlyTheirSha256Hashes() throws Exception {
        RefreshTokenRepository repository = mock(RefreshTokenRepository.class);
        User user = mock(User.class);
        when(repository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CountingSecureRandom secureRandom = new CountingSecureRandom();
        RefreshTokenIssuer issuer = new RefreshTokenIssuer(repository, new JwtProperties("test-key", 15, 7),
                Clock.fixed(NOW, ZoneOffset.UTC), secureRandom);

        String firstRaw = issuer.issueFor(user);
        String secondRaw = issuer.issueFor(user);

        assertEquals(2, secureRandom.callCount());
        assertNotEquals(firstRaw, secondRaw);
        assertTrue(firstRaw.matches("[A-Za-z0-9_-]{43}"));
        assertEquals(32, Base64.getUrlDecoder().decode(firstRaw).length);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        org.mockito.Mockito.verify(repository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<RefreshToken> persisted = captor.getAllValues();
        assertEquals(2, persisted.size());
        for (int index = 0; index < persisted.size(); index++) {
            String raw = index == 0 ? firstRaw : secondRaw;
            RefreshToken entity = persisted.get(index);
            String expectedHash = HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
            assertEquals(expectedHash, entity.getTokenHash());
            assertTrue(entity.getTokenHash().matches("[0-9a-f]{64}"));
            assertNotEquals(raw, entity.getTokenHash());
            assertSame(user, entity.getUser());
            assertEquals(LocalDateTime.ofInstant(NOW.plusSeconds(7 * 24 * 60 * 60L), ZoneOffset.UTC),
                    entity.getExpiresAt());
            assertFalse(entity.isRevoked());
        }
    }

    private static final class CountingSecureRandom extends SecureRandom {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public void nextBytes(byte[] bytes) {
            byte value = (byte) calls.incrementAndGet();
            java.util.Arrays.fill(bytes, value);
        }

        int callCount() {
            return calls.get();
        }
    }
}
