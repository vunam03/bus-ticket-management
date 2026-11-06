package com.busbooking.bus_ticket_management.repository;


import com.busbooking.bus_ticket_management.model.AccountSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface AccountSessionRepository extends JpaRepository<AccountSession, String> {
    Optional<AccountSession> findBySessionHashAndUserId(String hash, Long userId);
    Optional<AccountSession> findByIdAndUserId(String id, Long userId);
    List<AccountSession> findByUserIdAndCredentialVersionAndRevokedFalseAndExpiresAtAfterOrderByLastSeenAtDesc(
            Long userId, long version, Instant now);

    @Modifying
    @Query("update AccountSession s set s.revoked = true where s.userId = :owner and s.sessionHash <> :keep")
    int revokeOtherSessions(@Param("owner") Long owner, @Param("keep") String keep);

    @Modifying
    @Query("update AccountSession s set s.revoked = true where s.userId = :owner")
    int revokeAllForUser(@Param("owner") Long owner);

    // A concurrent revoke must not be undone by a stale last-seen update.
    @Modifying
    @Query("update AccountSession s set s.lastSeenAt = :now, s.expiresAt = :expiry "
            + "where s.id = :id and s.revoked = false and s.expiresAt > :now")
    int touchActive(@Param("id") String id, @Param("now") Instant now, @Param("expiry") Instant expiry);
    long deleteByExpiresAtBefore(Instant cutoff);
}
