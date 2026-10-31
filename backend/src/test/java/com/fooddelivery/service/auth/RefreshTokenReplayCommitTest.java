package com.fooddelivery.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fooddelivery.entity.RefreshToken;
import com.fooddelivery.entity.User;
import com.fooddelivery.repository.RefreshTokenRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.security.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class RefreshTokenReplayCommitTest {
    private static final String RAW = "A".repeat(43);

    @Test
    void replayRevocationCommitsBeforeLifecycleResultCanReachController() throws Exception {
        RefreshTokenRepository tokenRepository = mock(RefreshTokenRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        RefreshTokenIssuer issuer = mock(RefreshTokenIssuer.class);
        JwtService jwtService = mock(JwtService.class);
        String hash = HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
                .digest(RAW.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        User user = mock(User.class);
        when(user.getId()).thenReturn(27L);
        RefreshToken revokedToken = RefreshToken.create(user, hash,
                LocalDateTime.ofInstant(Instant.parse("2026-07-02T00:00:00Z"), ZoneOffset.UTC));
        revokedToken.revoke();
        when(tokenRepository.findUserIdByTokenHash(hash)).thenReturn(Optional.of(27L));
        when(userRepository.findByIdForUpdate(27L)).thenReturn(Optional.of(user));
        when(tokenRepository.findByTokenHashForUpdate(hash)).thenReturn(Optional.of(revokedToken));

        RefreshTokenLifecycleService target = new RefreshTokenLifecycleService(tokenRepository, userRepository,
                issuer, jwtService, Clock.fixed(Instant.parse("2026-07-01T00:00:00Z"), ZoneOffset.UTC));
        TrackingTransactionManager transactionManager = new TrackingTransactionManager();
        ProxyFactory proxyFactory = new ProxyFactory(target);
        proxyFactory.setProxyTargetClass(true);
        proxyFactory.addAdvice(new TransactionInterceptor(transactionManager,
                new AnnotationTransactionAttributeSource()));
        RefreshTokenLifecycleService transactionalService = (RefreshTokenLifecycleService) proxyFactory.getProxy();

        RefreshAttempt result = transactionalService.rotate(RAW);

        assertEquals(RefreshOutcome.REPLAY_DETECTED, result.outcome());
        assertEquals(1, transactionManager.commitCount);
        verify(tokenRepository).revokeAllUnrevokedForUser(27L);
    }

    private static final class TrackingTransactionManager extends AbstractPlatformTransactionManager {
        private int commitCount;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected boolean isExistingTransaction(Object transaction) {
            return false;
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commitCount++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
