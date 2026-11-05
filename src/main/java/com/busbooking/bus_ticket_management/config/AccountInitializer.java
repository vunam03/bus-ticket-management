package com.busbooking.bus_ticket_management.config;


import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;
import com.busbooking.bus_ticket_management.service.UserService;
import com.busbooking.bus_ticket_management.validator.StrongPasswordValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Idempotent role seed and opt-in first administrator. No hard-coded credentials. */
@Component
public class AccountInitializer implements CommandLineRunner {
    private final RoleRepository roles;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String username;
    private final String email;
    private final String password;

    public AccountInitializer(RoleRepository roles, UserRepository users, PasswordEncoder encoder,
            @Value("${app.bootstrap.admin.username:}") String username,
            @Value("${app.bootstrap.admin.email:}") String email,
            @Value("${app.bootstrap.admin.password:}") String password) {
        this.roles = roles;
        this.users = users;
        this.encoder = encoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        ensureRole(Role.CUSTOMER);
        Role admin = ensureRole(Role.ADMIN);
        if (username.isBlank() && email.isBlank() && password.isBlank()) {
            return;
        }
        String normalizedUsername = UserService.normalize(username);
        String normalizedEmail = UserService.normalize(email);
        if (!normalizedUsername.matches("[a-z0-9_]{4,30}")
                || normalizedEmail.length() > 254
                || !normalizedEmail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")
                || !new StrongPasswordValidator().isValid(password, null)) {
            throw new IllegalStateException("Cấu hình ADMIN không hợp lệ: kiểm tra username, email và mật khẩu mạnh.");
        }
        if (users.existsByUsername(normalizedUsername)) {
            // Do not silently promote an existing public account or overwrite its password.
            User existing = users.findByUsername(normalizedUsername).orElseThrow();
            if (!Role.ADMIN.equals(existing.getRole().getName()) || !existing.getEmail().equals(normalizedEmail)) {
                throw new IllegalStateException("Username ADMIN đã thuộc một tài khoản khác.");
            }
            return;
        }
        if (users.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("Email ADMIN đã được sử dụng.");
        }
        User user = new User();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setFullName("Quản trị viên");
        user.setPassword(encoder.encode(password));
        user.setRole(admin);
        user.setEnabled(true);
        users.saveAndFlush(user);
    }

    private Role ensureRole(String name) {
        return roles.findByName(name).orElseGet(() -> roles.saveAndFlush(new Role(name)));
    }
}
