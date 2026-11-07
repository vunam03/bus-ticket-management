package com.busbooking.bus_ticket_management.repository;


import com.busbooking.bus_ticket_management.model.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String hash);
    Optional<EmailVerificationToken> findFirstByUserIdAndPurposeOrderByCreatedAtDescIdDesc(
            Long userId, EmailTokenPurpose purpose);
    Optional<EmailVerificationToken> findFirstByUserIdAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
            Long userId, EmailTokenPurpose purpose, Instant now);

    @Modifying
    @Query("update EmailVerificationToken t set t.used = true "
            + "where t.user.id = :owner and t.purpose = :purpose and t.used = false")
    int invalidate(@Param("owner") Long owner, @Param("purpose") EmailTokenPurpose purpose);

    @Modifying
    @Query("update EmailVerificationToken t set t.used = true where t.user.id = :owner and t.used = false")
    int invalidateAll(@Param("owner") Long owner);
    long deleteByExpiresAtBefore(Instant cutoff);
}
