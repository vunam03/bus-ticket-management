package com.busbooking.bus_ticket_management.service;


import com.busbooking.bus_ticket_management.dto.ResetPasswordRequest;
import com.busbooking.bus_ticket_management.exception.InvalidResetTokenException;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class AuthService {
    public static final String RESET_RESPONSE =
            "Nếu email thuộc một tài khoản đang hoạt động, hướng dẫn đặt lại mật khẩu sẽ được gửi.";
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final UserService userService;
    private final ResetMailService mail;
    private final Clock clock;
    private final String baseUrl;
    private final SecurityAuditService audit;
    private final LoginAttemptService attempts;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, PasswordResetTokenRepository tokens,
                       UserService userService, ResetMailService mail, Clock clock, SecurityAuditService audit, LoginAttemptService attempts,
                       @Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.users = users;
        this.tokens = tokens;
        this.userService = userService;
        this.mail = mail;
        this.clock = clock;
        this.audit = audit;
        this.attempts = attempts;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public void requestPasswordReset(String email) {
        Optional<User> found = users.findByEmail(UserService.normalize(email));
        if (found.isEmpty()) {
            return;
        }
        User user = users.findLockedById(found.get().getId()).orElseThrow();
        entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!user.getEnabled()) {
            return;
        }
        tokens.invalidateForUser(user.getId());
        tokens.deleteExpiredBefore(clock.instant().minus(Duration.ofDays(1)));
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PasswordResetToken token = new PasswordResetToken();
        token.setTokenHash(digest(rawToken));
        token.setUser(user);
        token.setExpiresAt(clock.instant().plus(Duration.ofMinutes(15)));
        tokens.saveAndFlush(token);
        // Use configured origin, never the untrusted Host header.
        mail.sendPasswordReset(user.getEmail(), baseUrl + "/auth/reset-password?token=" + rawToken);
        audit.record(user.getId(), SecurityEventType.PASSWORD_RESET_REQUESTED);
    }

    @Transactional(readOnly = true)
    public boolean isResetTokenValid(String rawToken) {
        if (!validFormat(rawToken)) {
            return false;
        }
        return tokens.findByTokenHash(digest(rawToken))
                .filter(token -> !token.getUsed())
                .filter(token -> token.getExpiresAt().isAfter(clock.instant()))
                .filter(token -> token.getUser().getEnabled()).isPresent();
    }

    public void resetPassword(ResetPasswordRequest request) {
        UserService.requireMatching(request.getNewPassword(), request.getConfirmPassword());
        if (!validFormat(request.getToken())) {
            throw invalidToken();
        }
        PasswordResetToken token = tokens.findByTokenHash(digest(request.getToken()))
                .orElseThrow(this::invalidToken);
        User user = users.findLockedById(token.getUser().getId()).orElseThrow(this::invalidToken);
        entityManager.refresh(user, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        // Refresh after locking: another transaction may already have redeemed this token.
        entityManager.refresh(token, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (token.getUsed() || !token.getExpiresAt().isAfter(clock.instant()) || !user.getEnabled()) {
            throw invalidToken();
        }
        userService.replacePassword(user, request.getNewPassword());
        attempts.succeeded(user.getUsername());
        audit.record(user.getId(), SecurityEventType.PASSWORD_RESET);
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private InvalidResetTokenException invalidToken() {
        return new InvalidResetTokenException("token", "Link không hợp lệ, đã hết hạn hoặc đã được sử dụng.");
    }

    private boolean validFormat(String token) {
        return token != null && token.matches("[A-Za-z0-9_-]{43}");
    }

    public static String digest(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 không khả dụng.", exception);
        }
    }
}
