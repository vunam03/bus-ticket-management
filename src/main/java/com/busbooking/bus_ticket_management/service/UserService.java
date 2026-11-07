package com.busbooking.bus_ticket_management.service;


import com.busbooking.bus_ticket_management.dto.*;
import com.busbooking.bus_ticket_management.exception.*;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
@Transactional
public class UserService {
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final SecurityAuditService audit;
    private final EmailVerificationService emailVerification;

    public UserService(UserRepository users, RoleRepository roles,
                       PasswordResetTokenRepository tokens, PasswordEncoder encoder,
                       SecurityAuditService audit, EmailVerificationService emailVerification) {
        this.users = users;
        this.roles = roles;
        this.tokens = tokens;
        this.encoder = encoder;
        this.audit = audit;
        this.emailVerification = emailVerification;
    }

    public UserProfileResponse register(RegisterRequest request) {
        requireMatching(request.getPassword(), request.getConfirmPassword());
        String username = normalize(request.getUsername());
        String email = normalize(request.getEmail());
        if (users.existsByUsername(username)) {
            throw new UserAlreadyExistsException("username", "Tên đăng nhập đã được sử dụng.");
        }
        if (users.existsByEmail(email)) {
            throw new UserAlreadyExistsException("email", "Email đã được sử dụng.");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setFullName(request.getFullName().strip());
        user.setPhone(clean(request.getPhone()));
        user.setAddress(clean(request.getAddress()));
        user.setPassword(encoder.encode(request.getPassword()));
        user.setRole(roles.findByName(Role.CUSTOMER).orElseThrow(
                () -> new IllegalStateException("Chưa khởi tạo quyền CUSTOMER.")));
        user.setEnabled(true);
        user.setEmailVerified(false);
        try {
            // Catch concurrent registrations at the database unique constraint.
            users.saveAndFlush(user);
            audit.record(user.getId(), SecurityEventType.REGISTERED);
            emailVerification.sendRegistration(user);
            return toProfile(user);
        } catch (DataIntegrityViolationException exception) {
            throw new UserAlreadyExistsException("email", "Tên đăng nhập hoặc email đã được sử dụng.");
        }
    }

    @Transactional(readOnly = true)
    public UserProfileResponse profile(String username) {
        return toProfile(requireUser(username));
    }

    public void updateProfile(String username, UpdateProfileRequest request) {
        User user = lockUser(username);
        verifyCurrentPassword(user, request.getCurrentPassword());
        String email = normalize(request.getEmail());
        if (users.existsByEmailAndIdNot(email, user.getId())) {
            throw new UserAlreadyExistsException("email", "Email đã được sử dụng.");
        }
        boolean changedEmail = !user.getEmail().equals(email);
        user.setFullName(request.getFullName().strip());

        user.setPhone(clean(request.getPhone()));
        user.setAddress(clean(request.getAddress()));
        if (changedEmail) {
            emailVerification.requestEmailChange(user, email);
        }
        try {
            users.saveAndFlush(user);
            audit.record(user.getId(), SecurityEventType.PROFILE_UPDATED);
        } catch (DataIntegrityViolationException exception) {
            throw new UserAlreadyExistsException("email", "Email đã được sử dụng.");
        }
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        User user = lockUser(username);
        verifyCurrentPassword(user, request.getCurrentPassword());
        requireMatching(request.getNewPassword(), request.getConfirmPassword());
        replacePassword(user, request.getNewPassword());
        audit.record(user.getId(), SecurityEventType.PASSWORD_CHANGED);
    }

    public void replacePassword(User user, String newPassword) {
        if (encoder.matches(newPassword, user.getPassword())) {
            throw new AccountValidationException("newPassword", "Mật khẩu mới phải khác mật khẩu hiện tại.");
        }
        user.setPassword(encoder.encode(newPassword));
        user.setCredentialVersion(user.getCredentialVersion() + 1);
        tokens.invalidateForUser(user.getId());
        emailVerification.invalidateForUser(user.getId());
        users.saveAndFlush(user);
    }

    public static void requireMatching(String password, String confirmation) {
        if (password == null || !password.equals(confirmation)) {
            throw new AccountValidationException("confirmPassword", "Mật khẩu xác nhận không khớp.");
        }
    }

    @Transactional(readOnly = true)
    public User requireUser(String username) {
        return users.findByUsername(normalize(username)).orElseThrow(
                () -> new InvalidCredentialsException("currentPassword", "Tài khoản không khả dụng."));
    }

    private User lockUser(String username) {
        User found = requireUser(username);
        User locked = users.findLockedById(found.getId()).orElseThrow();
        entityManager.refresh(locked, jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if (!locked.getEnabled()) {
            throw new InvalidCredentialsException("currentPassword", "Tài khoản không khả dụng.");
        }
        return locked;
    }

    private void verifyCurrentPassword(User user, String password) {
        if (password == null || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72
                || !encoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("currentPassword", "Mật khẩu hiện tại không đúng.");
        }
    }

    public static String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    private UserProfileResponse toProfile(User user) {
        UserProfileResponse result = new UserProfileResponse();
        result.setId(user.getId());
        result.setUsername(user.getUsername());
        result.setFullName(user.getFullName());
        result.setEmail(user.getEmail());
        result.setPhone(user.getPhone());
        result.setAddress(user.getAddress());
        result.setRole(user.getRole().getName());
        result.setEnabled(user.getEnabled());
        result.setCreatedAt(user.getCreatedAt());
        result.setEmailVerified(user.getEmailVerified());
        result.setPendingEmail(emailVerification.pendingEmail(user));
        return result;
    }
}
