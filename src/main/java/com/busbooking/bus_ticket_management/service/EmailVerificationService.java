package com.busbooking.bus_ticket_management.service;


import com.busbooking.bus_ticket_management.exception.*;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class EmailVerificationService {
    public static final String RESEND_RESPONSE =
            "Nếu email thuộc tài khoản cần xác thực, hướng dẫn sẽ được gửi. Vui lòng chờ ít nhất 60 giây giữa các lần gửi.";
    private final EmailVerificationTokenRepository tokens;
    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final ResetMailService mail;
    private final SecurityAuditService audit;
    private final Clock clock;
    private final String baseUrl;
    private final SecureRandom random = new SecureRandom();

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public EmailVerificationService(EmailVerificationTokenRepository tokens, UserRepository users,
            PasswordResetTokenRepository resetTokens, ResetMailService mail,
            SecurityAuditService audit, Clock clock,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.tokens = tokens;
        this.users = users;
        this.resetTokens = resetTokens;
        this.mail = mail;
        this.audit = audit;
        this.clock = clock;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public void sendRegistration(User user) {
        issue(user, user.getEmail(), EmailTokenPurpose.REGISTRATION);
    }

    public void resendRegistration(String email) {
        var found = users.findByEmail(UserService.normalize(email));
        if (found.isEmpty()) return;
        User user = lock(found.get().getId());
        if (!user.getEnabled() || user.getEmailVerified() || coolingDown(user.getId(), EmailTokenPurpose.REGISTRATION)) {
            return;
        }
        issue(user, user.getEmail(), EmailTokenPurpose.REGISTRATION);
    }

    /** Caller already verified the current password and holds the account lock. */
    public void requestEmailChange(User user, String target) {
        if (!user.getEmailVerified()) {
            throw new AccountValidationException("email", "Vui lòng xác thực email hiện tại trước.");
        }
        if (coolingDown(user.getId(), EmailTokenPurpose.EMAIL_CHANGE)) {
            throw new AccountValidationException("email", "Vui lòng chờ 60 giây trước khi yêu cầu xác nhận email mới.");
        }
        issue(user, target, EmailTokenPurpose.EMAIL_CHANGE);
        audit.record(user.getId(), SecurityEventType.EMAIL_CHANGE_REQUESTED);
    }

    public void cancelEmailChange(Long owner) {
        lock(owner);
        tokens.invalidate(owner, EmailTokenPurpose.EMAIL_CHANGE);
        audit.record(owner, SecurityEventType.EMAIL_CHANGE_CANCELLED);
    }

    @Transactional(readOnly = true)
    public String pendingEmail(User user) {
        return tokens.findFirstByUserIdAndPurposeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
                        user.getId(), EmailTokenPurpose.EMAIL_CHANGE, clock.instant())
                .filter(token -> token.getCredentialVersion() == user.getCredentialVersion())
                .filter(token -> token.getOriginalEmail().equals(user.getEmail()))
                .map(EmailVerificationToken::getTargetEmail).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isValid(String raw) {
        if (!validFormat(raw)) return false;
        return tokens.findByTokenHash(AuthService.digest(raw)).filter(this::usable).isPresent();
    }

    /** GET only previews a link. A CSRF-protected POST consumes it. */
    public record ConfirmationResult(Long userId, EmailTokenPurpose purpose) {}

    public ConfirmationResult confirm(String raw) {
        if (!validFormat(raw)) throw invalid();
        EmailVerificationToken token = tokens.findByTokenHash(AuthService.digest(raw)).orElseThrow(this::invalid);
        User user = lock(token.getUser().getId());
        entityManager.refresh(token, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!usable(token)) throw invalid();
        if (users.existsByEmailAndIdNot(token.getTargetEmail(), user.getId())) {
            throw new InvalidEmailVerificationException("Email này không còn khả dụng. Vui lòng yêu cầu lại với địa chỉ khác.");
        }
        EmailTokenPurpose purpose = token.getPurpose();
        user.setEmail(token.getTargetEmail());
        user.setEmailVerified(true);
        if (purpose == EmailTokenPurpose.EMAIL_CHANGE) {
            user.setCredentialVersion(user.getCredentialVersion() + 1);
            resetTokens.invalidateForUser(user.getId());
        }
        tokens.invalidateAll(user.getId());
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new InvalidEmailVerificationException("Email này không còn khả dụng. Vui lòng yêu cầu lại.");
        }
        audit.record(user.getId(), purpose == EmailTokenPurpose.REGISTRATION
                ? SecurityEventType.EMAIL_VERIFIED : SecurityEventType.EMAIL_CHANGED);
        return new ConfirmationResult(user.getId(), purpose);
    }

    public void invalidateForUser(Long owner) {
        tokens.invalidateAll(owner);
    }

    private boolean usable(EmailVerificationToken token) {
        User user = token.getUser();
        return !token.getUsed() && token.getExpiresAt().isAfter(clock.instant()) && user.getEnabled()
                && token.getCredentialVersion() == user.getCredentialVersion()
                && token.getOriginalEmail().equals(user.getEmail())
                && (token.getPurpose() != EmailTokenPurpose.REGISTRATION || !user.getEmailVerified());
    }

    private void issue(User user, String target, EmailTokenPurpose purpose) {
        tokens.invalidate(user.getId(), purpose);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        EmailVerificationToken token = new EmailVerificationToken();
        token.setTokenHash(AuthService.digest(raw));
        token.setUser(user);
        token.setPurpose(purpose);
        token.setOriginalEmail(user.getEmail());
        token.setTargetEmail(target);
        token.setCredentialVersion(user.getCredentialVersion());
        token.setCreatedAt(clock.instant());
        token.setExpiresAt(clock.instant().plus(purpose == EmailTokenPurpose.REGISTRATION
                ? Duration.ofHours(24) : Duration.ofMinutes(30)));
        tokens.saveAndFlush(token);
        try {
            mail.sendEmailVerification(target, baseUrl + "/auth/verify-email?token=" + raw,
                    purpose == EmailTokenPurpose.EMAIL_CHANGE);
        } catch (RuntimeException exception) {
            // Roll back account/token changes; do not expose provider internals.
            throw new EmailDeliveryException();
        }
    }

    private boolean coolingDown(Long owner, EmailTokenPurpose purpose) {
        return tokens.findFirstByUserIdAndPurposeOrderByCreatedAtDescIdDesc(owner, purpose)
                .map(token -> token.getCreatedAt().plusSeconds(60).isAfter(clock.instant())).orElse(false);
    }

    private User lock(Long owner) {
        User user = users.findLockedById(owner).orElseThrow(this::invalid);
        entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        return user;
    }

    private boolean validFormat(String raw) {
        return raw != null && raw.matches("[A-Za-z0-9_-]{43}");
    }

    private InvalidEmailVerificationException invalid() {
        return new InvalidEmailVerificationException("Link xác nhận không hợp lệ, đã hết hạn hoặc đã được sử dụng.");
    }
}
