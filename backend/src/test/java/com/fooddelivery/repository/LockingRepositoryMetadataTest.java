package com.fooddelivery.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.LockModeType;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

class LockingRepositoryMetadataTest {
    @Test
    void refreshTokenRepositoryHasPlainLookupAndPessimisticLockingLookup() throws Exception {
        assertNotNull(RefreshTokenRepository.class.getMethod("findByTokenHash", String.class));
        Method ownerLookup = RefreshTokenRepository.class.getMethod("findUserIdByTokenHash", String.class);
        assertEquals("select rt.user.id from RefreshToken rt where rt.tokenHash = :tokenHash",
                ownerLookup.getAnnotation(Query.class).value());

        Method lockedLookup = RefreshTokenRepository.class.getMethod("findByTokenHashForUpdate", String.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lockedLookup.getAnnotation(Lock.class).value());
        assertEquals("select rt from RefreshToken rt where rt.tokenHash = :tokenHash",
                lockedLookup.getAnnotation(Query.class).value());
        Method revokeAll = RefreshTokenRepository.class.getMethod("revokeAllUnrevokedForUser", Long.class);
        assertEquals("update RefreshToken rt set rt.revoked = true where rt.user.id = :userId and rt.revoked = false",
                revokeAll.getAnnotation(Query.class).value());
    }

    @Test
    void userRepositoryProvidesPessimisticUserRowLock() throws Exception {
        Method lockedLookup = UserRepository.class.getMethod("findByIdForUpdate", Long.class);
        assertEquals(LockModeType.PESSIMISTIC_WRITE, lockedLookup.getAnnotation(Lock.class).value());
        assertEquals("select u from User u where u.id = :userId",
                lockedLookup.getAnnotation(Query.class).value());
        assertNotNull(UserRepository.class.getMethod("findByIdWithRoles", Long.class));
    }
}
