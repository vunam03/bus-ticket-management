package com.busbooking.bus_ticket_management.repository;


import com.busbooking.bus_ticket_management.model.PasswordResetToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update PasswordResetToken t set t.used = true where t.user.id = :userId and t.used = false")
    int invalidateForUser(@Param("userId") Long userId);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") Instant cutoff);
}
