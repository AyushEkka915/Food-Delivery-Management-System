package com.fooddelivery.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fooddelivery.config.JwtProperties;
import com.fooddelivery.dto.auth.LoginResponse;
import com.fooddelivery.entity.RefreshToken;
import com.fooddelivery.entity.Role;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserStatus;
import com.fooddelivery.repository.RefreshTokenRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class RefreshTokenLifecycleServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");
    private static final String RAW_TOKEN = "A".repeat(43);

    private RefreshTokenRepository tokenRepository;
    private UserRepository userRepository;
    private RefreshTokenIssuer tokenIssuer;
    private JwtService jwtService;
    private RefreshTokenLifecycleService service;
    private User user;
    private RefreshToken token;
    private String tokenHash;

    @BeforeEach
    void setUp() throws Exception {
        tokenRepository = mock(RefreshTokenRepository.class);
        userRepository = mock(UserRepository.class);
        tokenIssuer = mock(RefreshTokenIssuer.class);
        jwtService = mock(JwtService.class);
        service = new RefreshTokenLifecycleService(tokenRepository, userRepository, tokenIssuer,
                jwtService, Clock.fixed(NOW, ZoneOffset.UTC));

        Role role = mock(Role.class);
        when(role.getName()).thenReturn("CUSTOMER");
        user = mock(User.class);
        when(user.getId()).thenReturn(27L);
        when(user.getEmail()).thenReturn("customer@example.com");
        when(user.getPasswordHashForAuthentication()).thenReturn("unused-test-hash");
        when(user.getRoles()).thenReturn(Set.of(role));
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        tokenHash = sha256(RAW_TOKEN);
        token = RefreshToken.create(user, tokenHash, LocalDateTime.ofInstant(NOW.plusSeconds(3600), ZoneOffset.UTC));
        when(tokenRepository.findUserIdByTokenHash(tokenHash)).thenReturn(Optional.of(27L));
        when(userRepository.findByIdForUpdate(27L)).thenReturn(Optional.of(user));
        when(tokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(token));
        when(userRepository.findByIdWithRoles(27L)).thenReturn(Optional.of(user));
        when(tokenIssuer.issueFor(user)).thenReturn("replacement-refresh-token-value-0123456789");
        when(jwtService.generateAccessToken(any())).thenReturn("new-access-token");
    }

    @Test
    void rotatesValidTokenUsingUserThenTokenLockAndCurrentRoles() {
        RefreshAttempt attempt = service.rotate(RAW_TOKEN);

        assertEquals(RefreshOutcome.ROTATED, attempt.outcome());
        assertTrue(token.isRevoked());
        assertEquals("new-access-token", attempt.session().accessTokenResponse().accessToken());
        assertEquals("replacement-refresh-token-value-0123456789", attempt.session().refreshToken());
        verify(tokenIssuer).issueFor(user);
        verify(jwtService).generateAccessToken(any(com.fooddelivery.security.AuthenticatedUser.class));
        InOrder order = inOrder(tokenRepository, userRepository);
        order.verify(tokenRepository).findUserIdByTokenHash(tokenHash);
        order.verify(userRepository).findByIdForUpdate(27L);
        order.verify(tokenRepository).findByTokenHashForUpdate(tokenHash);
        order.verify(userRepository).findByIdWithRoles(27L);
    }

    @Test
    void rejectsMalformedAndUnknownTokensWithoutLockingUsers() {
        assertEquals(RefreshOutcome.REJECTED, service.rotate("not-a-refresh-token").outcome());
        verify(tokenRepository, never()).findUserIdByTokenHash(any());

        when(tokenRepository.findUserIdByTokenHash(tokenHash)).thenReturn(Optional.empty());
        assertEquals(RefreshOutcome.REJECTED, service.rotate(RAW_TOKEN).outcome());
        verify(userRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void expiresOldTokenWithoutTreatingItAsReplay() {
        RefreshToken expired = RefreshToken.create(user, tokenHash,
                LocalDateTime.ofInstant(NOW.minusSeconds(1), ZoneOffset.UTC));
        when(tokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(expired));

        RefreshAttempt attempt = service.rotate(RAW_TOKEN);

        assertEquals(RefreshOutcome.REJECTED, attempt.outcome());
        assertTrue(expired.isRevoked());
        verify(tokenRepository, never()).revokeAllUnrevokedForUser(any());
        verify(tokenIssuer, never()).issueFor(any());
    }

    @Test
    void expiredRevokedTokenIsNotTreatedAsReplay() {
        RefreshToken expiredRevoked = RefreshToken.create(user, tokenHash,
                LocalDateTime.ofInstant(NOW.minusSeconds(1), ZoneOffset.UTC));
        expiredRevoked.revoke();
        when(tokenRepository.findByTokenHashForUpdate(tokenHash)).thenReturn(Optional.of(expiredRevoked));

        assertEquals(RefreshOutcome.REJECTED, service.rotate(RAW_TOKEN).outcome());
        verify(tokenRepository, never()).revokeAllUnrevokedForUser(any());
    }

    @Test
    void replayRevokesAllUnrevokedTokensForTheUserAndReturnsAnInternalOutcome() {
        token.revoke();

        RefreshAttempt attempt = service.rotate(RAW_TOKEN);

        assertEquals(RefreshOutcome.REPLAY_DETECTED, attempt.outcome());
        assertEquals(null, attempt.session());
        verify(tokenRepository).revokeAllUnrevokedForUser(27L);
        verify(tokenIssuer, never()).issueFor(any());
    }

    @Test
    void inactiveUserCannotRefreshAndAllTheirTokensAreRevoked() {
        when(user.getStatus()).thenReturn(UserStatus.SUSPENDED);

        RefreshAttempt attempt = service.rotate(RAW_TOKEN);

        assertEquals(RefreshOutcome.REJECTED, attempt.outcome());
        verify(tokenRepository).revokeAllUnrevokedForUser(27L);
        verify(tokenIssuer, never()).issueFor(any());
    }

    @Test
    void logoutRevokesOnlyThePresentedActiveTokenWithoutReplayRevocation() {
        RefreshToken unrelated = RefreshToken.create(user, "b".repeat(64),
                LocalDateTime.ofInstant(NOW.plusSeconds(7200), ZoneOffset.UTC));

        service.logout(RAW_TOKEN);

        assertTrue(token.isRevoked());
        assertFalse(unrelated.isRevoked());
        verify(tokenRepository, never()).revokeAllUnrevokedForUser(any());
        InOrder order = inOrder(tokenRepository, userRepository);
        order.verify(tokenRepository).findUserIdByTokenHash(tokenHash);
        order.verify(userRepository).findByIdForUpdate(27L);
        order.verify(tokenRepository).findByTokenHashForUpdate(tokenHash);
    }

    @Test
    void logoutIsIdempotentForMissingUnknownMalformedAndAlreadyRevokedTokens() {
        service.logout(null);
        service.logout("");
        service.logout("malformed");
        verify(tokenRepository, never()).findUserIdByTokenHash(any());

        when(tokenRepository.findUserIdByTokenHash(tokenHash)).thenReturn(Optional.empty());
        service.logout(RAW_TOKEN);
        when(tokenRepository.findUserIdByTokenHash(tokenHash)).thenReturn(Optional.of(27L));
        token.revoke();
        service.logout(RAW_TOKEN);

        verify(tokenRepository, never()).revokeAllUnrevokedForUser(any());
        verify(tokenIssuer, never()).issueFor(any());
    }

    @Test
    void concurrentRequestsForTheSameTokenCanOnlyRotateOnce() throws Exception {
        ReentrantLock simulatedUserRowLock = new ReentrantLock();
        when(userRepository.findByIdForUpdate(27L)).thenAnswer(invocation -> {
            simulatedUserRowLock.lock();
            return Optional.of(user);
        });
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<RefreshOutcome> first = executor.submit(() -> rotateUnderSimulatedRowLock(
                    simulatedUserRowLock, ready, start));
            Future<RefreshOutcome> second = executor.submit(() -> rotateUnderSimulatedRowLock(
                    simulatedUserRowLock, ready, start));
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();

            RefreshOutcome firstOutcome = first.get(5, TimeUnit.SECONDS);
            RefreshOutcome secondOutcome = second.get(5, TimeUnit.SECONDS);

            assertTrue(Set.of(firstOutcome, secondOutcome).contains(RefreshOutcome.ROTATED));
            assertEquals(1, java.util.stream.Stream.of(firstOutcome, secondOutcome)
                    .filter(outcome -> outcome == RefreshOutcome.ROTATED).count());
            assertTrue(Set.of(firstOutcome, secondOutcome).contains(RefreshOutcome.REPLAY_DETECTED));
            verify(tokenRepository).revokeAllUnrevokedForUser(27L);
        } finally {
            executor.shutdownNow();
        }
    }

    private RefreshOutcome rotateUnderSimulatedRowLock(ReentrantLock rowLock, CountDownLatch ready,
                                                        CountDownLatch start) throws Exception {
        ready.countDown();
        start.await();
        try {
            return service.rotate(RAW_TOKEN).outcome();
        } finally {
            rowLock.unlock();
        }
    }

    private static String sha256(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8)));
    }
}
