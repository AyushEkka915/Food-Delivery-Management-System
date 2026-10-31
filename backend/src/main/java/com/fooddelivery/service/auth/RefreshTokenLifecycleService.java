package com.fooddelivery.service.auth;

import com.fooddelivery.dto.auth.LoginResponse;
import com.fooddelivery.entity.RefreshToken;
import com.fooddelivery.entity.User;
import com.fooddelivery.entity.enums.UserStatus;
import com.fooddelivery.repository.RefreshTokenRepository;
import com.fooddelivery.repository.UserRepository;
import com.fooddelivery.security.AuthenticatedUser;
import com.fooddelivery.security.JwtService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenLifecycleService {
    private static final int GENERATED_TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenIssuer refreshTokenIssuer;
    private final JwtService jwtService;
    private final Clock clock;

    public RefreshTokenLifecycleService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository,
                                        RefreshTokenIssuer refreshTokenIssuer, JwtService jwtService, Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshTokenIssuer = refreshTokenIssuer;
        this.jwtService = jwtService;
        this.clock = clock;
    }

    @Transactional
    public RefreshAttempt rotate(String rawToken) {
        if (!hasExpectedTokenFormat(rawToken)) {
            return rejected();
        }

        String tokenHash = sha256Hex(rawToken);
        Optional<Long> userIdResult = refreshTokenRepository.findUserIdByTokenHash(tokenHash);
        if (userIdResult.isEmpty()) {
            return rejected();
        }
        Long userId = userIdResult.get();

        // Every refresh operation locks the user before the token to serialize rotations for that user.
        Optional<User> lockedUserResult = userRepository.findByIdForUpdate(userId);
        if (lockedUserResult.isEmpty()) {
            return rejected();
        }
        User lockedUser = lockedUserResult.get();

        // The scalar preliminary lookup did not load the token entity into this persistence context.
        Optional<RefreshToken> tokenResult = refreshTokenRepository.findByTokenHashForUpdate(tokenHash);
        if (tokenResult.isEmpty()) {
            return rejected();
        }
        RefreshToken currentToken = tokenResult.get();
        if (!userId.equals(currentToken.getUser().getId())) {
            return rejected();
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        if (!currentToken.getExpiresAt().isAfter(now)) {
            currentToken.revoke();
            return rejected();
        }

        if (currentToken.isRevoked()) {
            refreshTokenRepository.revokeAllUnrevokedForUser(userId);
            // Returning lets the transaction commit the user-wide revocation before the controller sends 401.
            return new RefreshAttempt(RefreshOutcome.REPLAY_DETECTED, null);
        }

        if (lockedUser.getStatus() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeAllUnrevokedForUser(userId);
            return rejected();
        }

        User userWithCurrentRoles = userRepository.findByIdWithRoles(userId).orElse(null);
        if (userWithCurrentRoles == null || userWithCurrentRoles.getStatus() != UserStatus.ACTIVE) {
            refreshTokenRepository.revokeAllUnrevokedForUser(userId);
            return rejected();
        }

        currentToken.revoke();
        String replacementRawToken = refreshTokenIssuer.issueFor(userWithCurrentRoles);
        String accessToken = jwtService.generateAccessToken(AuthenticatedUser.from(userWithCurrentRoles));
        LoginResponse accessResponse = new LoginResponse(accessToken, "Bearer",
                JwtService.ACCESS_TOKEN_LIFETIME.toSeconds());
        LoginSession session = new LoginSession(accessResponse, replacementRawToken);
        return new RefreshAttempt(RefreshOutcome.ROTATED, session);
    }

    /** Idempotently revokes only the presented refresh token; logout is not replay detection. */
    @Transactional
    public void logout(String rawToken) {
        if (!hasExpectedTokenFormat(rawToken)) {
            return;
        }

        String tokenHash = sha256Hex(rawToken);
        Optional<Long> userIdResult = refreshTokenRepository.findUserIdByTokenHash(tokenHash);
        if (userIdResult.isEmpty()) {
            return;
        }
        Long userId = userIdResult.get();

        if (userRepository.findByIdForUpdate(userId).isEmpty()) {
            return;
        }

        Optional<RefreshToken> tokenResult = refreshTokenRepository.findByTokenHashForUpdate(tokenHash);
        if (tokenResult.isEmpty()) {
            return;
        }

        RefreshToken token = tokenResult.get();
        if (userId.equals(token.getUser().getId()) && !token.isRevoked()) {
            token.revoke();
        }
    }

    private static RefreshAttempt rejected() {
        return new RefreshAttempt(RefreshOutcome.REJECTED, null);
    }

    private static boolean hasExpectedTokenFormat(String token) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            return false;
        }
        try {
            return Base64.getUrlDecoder().decode(token).length == GENERATED_TOKEN_BYTES;
        } catch (IllegalArgumentException exception) {
            return false;
        }
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
