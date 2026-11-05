package com.busbooking.bus_ticket_management.service;


import com.busbooking.bus_ticket_management.model.LoginAttempt;
import com.busbooking.bus_ticket_management.repository.LoginAttemptRepository;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.stream.IntStream;

/** Fixed-size striped monitors serialize attempts per normalized login on this instance. */
@Service
public class LoginAttemptService {
    public static final int MAX_FAILURES = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(15);
    private final LoginAttemptRepository attempts;
    private final Clock clock;
    private final Object[] stripes = IntStream.range(0, 64).mapToObj(i -> new Object()).toArray();

    public LoginAttemptService(LoginAttemptRepository attempts, Clock clock) {
        this.attempts = attempts;
        this.clock = clock;
    }

    public Object monitor(String username) {
        String key = key(username);
        return stripes[Math.floorMod(key.hashCode(), stripes.length)];
    }

    @Transactional(readOnly = true)
    public long remainingSeconds(String username) {
        return attempts.findById(key(username))
                .filter(attempt -> attempt.getLockedUntil() != null)
                .map(attempt -> Math.max(0L,
                        (Duration.between(clock.instant(), attempt.getLockedUntil()).toMillis() + 999) / 1000))
                .orElse(0L);
    }

    public void checkAllowed(String username) {
        if (remainingSeconds(username) > 0) {
            throw new LockedException("Tạm chặn đăng nhập. Vui lòng thử lại sau.");
        }
    }

    @Transactional
    public void failed(String username) {
        Instant now = clock.instant();
        LoginAttempt attempt = attempts.findById(key(username)).orElseGet(() -> {
            LoginAttempt fresh = new LoginAttempt();
            fresh.setUsernameHash(key(username));
            fresh.setWindowEndsAt(now.plus(LOCK_DURATION));
            return fresh;
        });
        if ((attempt.getLockedUntil() != null && !attempt.getLockedUntil().isAfter(now))
                || (attempt.getLockedUntil() == null && !attempt.getWindowEndsAt().isAfter(now))) {
            attempt.setFailures(0);
            attempt.setLockedUntil(null);
            attempt.setWindowEndsAt(now.plus(LOCK_DURATION));
        }
        attempt.setFailures(attempt.getFailures() + 1);
        if (attempt.getFailures() >= MAX_FAILURES) {
            attempt.setLockedUntil(now.plus(LOCK_DURATION));
        }
        attempts.saveAndFlush(attempt);
    }

    @Transactional
    public void succeeded(String username) {
        attempts.deleteById(key(username));
    }

    private String key(String username) {
        return AuthService.digest(UserService.normalize(username));
    }
}
