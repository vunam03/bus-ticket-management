package com.busbooking.bus_ticket_management;

import com.busbooking.bus_ticket_management.dto.*;
import com.busbooking.bus_ticket_management.exception.*;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;
import com.busbooking.bus_ticket_management.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real MVC + security filters + JPA transactions. SMTP alone is substituted. */
@SpringBootTest
@ActiveProfiles("test")
@Import(AccountModuleIntegrationTests.MailConfiguration.class)
class AccountModuleIntegrationTests {
    @Autowired WebApplicationContext context;
    @Autowired UserService userService;
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PasswordResetTokenRepository tokens;
    @Autowired PasswordEncoder encoder;
    @Autowired SecurityEventRepository auditRecords;
    @Autowired AccountSessionRepository sessionRecords;
    @Autowired LoginAttemptRepository attemptRecords;
    @Autowired LoginAttemptService loginAttempts;
    @Autowired SecurityAuditService auditService;
    @Autowired AccountSessionService accountSessions;

    @Autowired CapturingMail mail;
    MockMvc mvc;
    String clientIp;
    static final String PASSWORD = "StrongPass1!";
    static final String NEXT_PASSWORD = "DifferentPass2@";

    @TestConfiguration
    static class MailConfiguration {
        @Bean
        @Primary
        CapturingMail testMail() {
            return new CapturingMail();
        }
    }

    static class CapturingMail implements ResetMailService {
        final List<String> links = new CopyOnWriteArrayList<>();
        boolean fail;
        @Override
        public void sendPasswordReset(String email, String url) {
            if (fail) throw new IllegalStateException("Simulated delivery failure");
            links.add(url);
        }
    }

    @BeforeEach
    void setUp() {
        auditRecords.deleteAll();
        sessionRecords.deleteAll();
        attemptRecords.deleteAll();
        tokens.deleteAll();
        users.deleteAll();
        mail.links.clear();
        mail.fail = false;
        clientIp = UUID.randomUUID().toString();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/").with(request -> {
                    request.setRemoteAddr(clientIp);
                    return request;
                }))
                .build();
    }

    RegisterRequest registration(String username, String email) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setEmail(email);
        request.setFullName("Bùi Lê Minh");
        request.setPhone("0901234567");
        request.setAddress("Hà Nội");
        request.setPassword(PASSWORD);
        request.setConfirmPassword(PASSWORD);
        return request;
    }

    User createUser() {
        userService.register(registration("minh_test", "minh@example.com"));
        return users.findByUsername("minh_test").orElseThrow();
    }

    MockHttpSession login() throws Exception {
        return (MockHttpSession) mvc.perform(post("/auth/login").with(csrf())
                        .param("username", "MINH_TEST").param("password", PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/user/profile"))
                .andReturn().getRequest().getSession(false);
    }

    String issueToken() {
        auth.requestPasswordReset("minh@example.com");
        String url = mail.links.get(mail.links.size() - 1);
        return url.substring(url.indexOf("token=") + 6);
    }

    ResetPasswordRequest reset(String token) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        request.setNewPassword(NEXT_PASSWORD);
        request.setConfirmPassword(NEXT_PASSWORD);
        return request;
    }

    MockHttpServletRequestBuilder validRegistrationPost() {
        return post("/auth/register").with(csrf())
                .param("username", "new_user").param("fullName", "Người dùng mới")
                .param("email", "new@example.com").param("phone", "0901234567")
                .param("address", "Hải Phòng")
                .param("password", PASSWORD).param("confirmPassword", PASSWORD);
    }

    @Test
    void registrationStoresHashAndCustomerAuthority() throws Exception {
        mvc.perform(validRegistrationPost().param("role", "ADMIN").param("enabled", "false"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/auth/login"));
        User saved = users.findByUsername("new_user").orElseThrow();
        assertThat(saved.getRole().getName()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getEnabled()).isTrue();
        assertThat(saved.getPassword()).startsWith("$2").isNotEqualTo(PASSWORD);
        assertThat(encoder.matches(PASSWORD, saved.getPassword())).isTrue();
    }

    @Test
    void registrationRejectsWeakPasswordAndInvalidEmail() throws Exception {
        mvc.perform(validRegistrationPost().with(request -> {
                    request.setParameter("password", "weak");
                    request.setParameter("email", "invalid");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registerRequest", "password", "email"));
        assertThat(users.count()).isZero();
    }

    @Test
    void registrationRejectsMismatchedConfirmation() throws Exception {
        mvc.perform(validRegistrationPost().with(request -> {
                    request.setParameter("confirmPassword", "WrongPassword1!");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("registerRequest", "confirmPassword"));
        assertThat(users.count()).isZero();
    }

    @Test
    void usernamesAndEmailsAreNormalizedAndUnique() {
        userService.register(registration("MiNh_Test", "MINH@EXAMPLE.COM"));
        assertThat(users.findByUsername("minh_test")).isPresent();
        assertThatThrownBy(() -> userService.register(registration("MINH_TEST", "other@example.com")))
                .isInstanceOf(UserAlreadyExistsException.class);
        assertThatThrownBy(() -> userService.register(registration("someone", "minh@example.com")))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void publicPagesRenderWithoutAuthentication() throws Exception {
        for (String url : List.of("/", "/auth/login", "/auth/register", "/auth/forgot-password",
                "/auth/reset-password", "/css/account.css", "/js/account.js")) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    void csrfIsRequiredForRegistrationAndLogin() throws Exception {
        mvc.perform(post("/auth/register")).andExpect(status().isForbidden());
        mvc.perform(post("/auth/login")).andExpect(status().isForbidden());
        assertThat(users.count()).isZero();
    }

    @Test
    void anonymousProfileRedirectsToLogin() throws Exception {
        mvc.perform(get("/user/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void loginCreatesSessionAndProfileRendersWithoutPasswordHash() throws Exception {
        User user = createUser();
        MockHttpSession session = login();
        mvc.perform(get("/user/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bùi Lê Minh")))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString(user.getPassword()))));
        mvc.perform(get("/user/change-password").session(session)).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordDoesNotAuthenticate() throws Exception {
        createUser();
        mvc.perform(post("/auth/login").with(csrf())
                        .param("username", "minh_test").param("password", "wrong"))
                .andExpect(redirectedUrl("/auth/login?error"));
    }

    @Test
    void disabledAccountCannotLogin() throws Exception {
        User user = createUser();
        user.setEnabled(false);
        users.saveAndFlush(user);
        mvc.perform(post("/auth/login").with(csrf())
                        .param("username", "minh_test").param("password", PASSWORD))
                .andExpect(redirectedUrl("/auth/login?error"));
    }

    @Test
    void customerCannotAccessAdminButAdminCan() throws Exception {
        User user = createUser();
        MockHttpSession customer = login();
        mvc.perform(get("/admin/account").session(customer)).andExpect(status().isForbidden());
        user.setRole(roles.findByName(Role.ADMIN).orElseThrow());
        users.saveAndFlush(user);
        MockHttpSession admin = login();
        mvc.perform(get("/admin/account").session(admin))
                .andExpect(status().isOk()).andExpect(view().name("admin/account"));
    }

    @Test
    void roleChangeInvalidatesExistingSession() throws Exception {
        User user = createUser();
        MockHttpSession session = login();
        user.setRole(roles.findByName(Role.ADMIN).orElseThrow());
        users.saveAndFlush(user);
        mvc.perform(get("/user/profile").session(session))
                .andExpect(redirectedUrl("/auth/login?expired"));
    }

    @Test
    void disablementInvalidatesExistingSession() throws Exception {
        User user = createUser();
        MockHttpSession session = login();
        user.setEnabled(false);
        users.saveAndFlush(user);
        mvc.perform(get("/user/profile").session(session))
                .andExpect(redirectedUrl("/auth/login?expired"));
    }

    @Test
    void profileUpdateRequiresCurrentPasswordAndCannotEditRole() throws Exception {
        createUser();
        MockHttpSession session = login();
        mvc.perform(post("/user/profile").session(session).with(csrf())
                        .param("fullName", "Tên mới").param("email", "changed@example.com")
                        .param("currentPassword", "wrong"))
                .andExpect(model().attributeHasFieldErrors("updateProfileRequest", "currentPassword"));
        assertThat(users.findByUsername("minh_test").orElseThrow().getEmail()).isEqualTo("minh@example.com");
        mvc.perform(post("/user/profile").session(session).with(csrf())
                        .param("fullName", "Tên mới").param("email", "changed@example.com")
                        .param("currentPassword", PASSWORD).param("role", "ADMIN")
                        .param("username", "attacker"))
                .andExpect(redirectedUrl("/user/profile"));
        User saved = users.findByUsername("minh_test").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("changed@example.com");
        assertThat(saved.getRole().getName()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void profileCannotTakeAnotherAccountsEmail() throws Exception {
        createUser();
        userService.register(registration("other_user", "other@example.com"));
        mvc.perform(post("/user/profile").session(login()).with(csrf())
                        .param("fullName", "Tên mới").param("email", "other@example.com")
                        .param("currentPassword", PASSWORD))
                .andExpect(model().attributeHasFieldErrors("updateProfileRequest", "email"));
    }

    @Test
    void passwordChangeRequiresCurrentPasswordAndMatchingConfirmation() throws Exception {
        createUser();
        MockHttpSession session = login();
        mvc.perform(post("/user/change-password").session(session).with(csrf())
                        .param("currentPassword", "wrong").param("newPassword", NEXT_PASSWORD)
                        .param("confirmPassword", NEXT_PASSWORD))
                .andExpect(model().attributeHasFieldErrors("changePasswordRequest", "currentPassword"));
        mvc.perform(post("/user/change-password").session(session).with(csrf())
                        .param("currentPassword", PASSWORD).param("newPassword", NEXT_PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(model().attributeHasFieldErrors("changePasswordRequest", "confirmPassword"));
        assertThat(encoder.matches(PASSWORD, users.findByUsername("minh_test").orElseThrow().getPassword())).isTrue();
    }

    @Test
    void passwordChangeLogsOutCurrentAndOtherSessions() throws Exception {
        createUser();
        MockHttpSession first = login();
        MockHttpSession second = login();
        mvc.perform(post("/user/change-password").session(first).with(csrf())
                        .param("currentPassword", PASSWORD).param("newPassword", NEXT_PASSWORD)
                        .param("confirmPassword", NEXT_PASSWORD))
                .andExpect(redirectedUrl("/auth/login?passwordChanged"));
        mvc.perform(get("/user/profile").session(second)).andExpect(redirectedUrl("/auth/login?expired"));
        mvc.perform(post("/auth/login").with(csrf())
                        .param("username", "minh_test").param("password", NEXT_PASSWORD))
                .andExpect(redirectedUrl("/user/profile"));
    }

    @Test
    void resetUsesDigestAndCanOnlyBeRedeemedOnce() {
        createUser();
        String raw = issueToken();
        PasswordResetToken saved = tokens.findAll().get(0);
        assertThat(saved.getTokenHash()).isNotEqualTo(raw).isEqualTo(AuthService.digest(raw));
        assertThat(auth.isResetTokenValid(raw)).isTrue();
        auth.resetPassword(reset(raw));
        assertThat(auth.isResetTokenValid(raw)).isFalse();
        assertThatThrownBy(() -> auth.resetPassword(reset(raw))).isInstanceOf(InvalidResetTokenException.class);
        assertThat(encoder.matches(NEXT_PASSWORD, users.findByUsername("minh_test").orElseThrow().getPassword())).isTrue();
    }

    @Test
    void resetPageAndPostRenderAndValidate() throws Exception {
        createUser();
        String raw = issueToken();
        mvc.perform(get("/auth/reset-password").param("token", raw))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("newPassword")));
        mvc.perform(post("/auth/reset-password").with(csrf()).param("token", raw)
                        .param("newPassword", "weak").param("confirmPassword", "weak"))
                .andExpect(model().attributeHasFieldErrors("resetPasswordRequest", "newPassword"));
        mvc.perform(post("/auth/reset-password").with(csrf()).param("token", raw)
                        .param("newPassword", NEXT_PASSWORD).param("confirmPassword", NEXT_PASSWORD))
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    void expiredTokenCannotResetPassword() {
        createUser();
        String raw = issueToken();
        PasswordResetToken token = tokens.findAll().get(0);
        token.setExpiresAt(Instant.now().minusSeconds(1));
        tokens.saveAndFlush(token);
        assertThat(auth.isResetTokenValid(raw)).isFalse();
        assertThatThrownBy(() -> auth.resetPassword(reset(raw))).isInstanceOf(InvalidResetTokenException.class);
    }

    @Test
    void newResetRequestRevokesPreviousLink() {
        createUser();
        String first = issueToken();
        String second = issueToken();
        assertThat(auth.isResetTokenValid(first)).isFalse();
        assertThat(auth.isResetTokenValid(second)).isTrue();
    }

    @Test
    void resetRevokesExistingLoginSessions() throws Exception {
        createUser();
        MockHttpSession session = login();
        auth.resetPassword(reset(issueToken()));
        mvc.perform(get("/user/profile").session(session)).andExpect(redirectedUrl("/auth/login?expired"));
    }

    @Test
    void emailChangeRevokesLinksSentToOldEmail() {
        createUser();
        String token = issueToken();
        UpdateProfileRequest update = new UpdateProfileRequest();
        update.setFullName("Bùi Lê Minh");
        update.setEmail("new@example.com");
        update.setCurrentPassword(PASSWORD);
        userService.updateProfile("minh_test", update);
        assertThat(auth.isResetTokenValid(token)).isFalse();
    }

    @Test
    void unknownAndDisabledEmailsSendNothing() {
        User user = createUser();
        auth.requestPasswordReset("unknown@example.com");
        user.setEnabled(false);
        users.saveAndFlush(user);
        auth.requestPasswordReset("minh@example.com");
        assertThat(mail.links).isEmpty();
    }

    @Test
    void forgotPasswordResponseDoesNotRevealAccountExistence() throws Exception {
        createUser();
        for (String email : List.of("minh@example.com", "unknown@example.com")) {
            mvc.perform(post("/auth/forgot-password").with(csrf()).param("email", email))
                    .andExpect(redirectedUrl("/auth/forgot-password"))
                    .andExpect(flash().attribute("successMessage", AuthService.RESET_RESPONSE));
        }
    }

    @Test
    void failedMailRollsBackTokenAndStillReturnsGenericResponse() throws Exception {
        createUser();
        mail.fail = true;
        mvc.perform(post("/auth/forgot-password").with(csrf()).param("email", "minh@example.com"))
                .andExpect(redirectedUrl("/auth/forgot-password"))
                .andExpect(flash().attribute("successMessage", AuthService.RESET_RESPONSE));
        assertThat(tokens.count()).isZero();
    }

    @Test
    void invalidTokenAndInvalidEmailShowFriendlyErrors() throws Exception {
        mvc.perform(get("/auth/reset-password").param("token", "bad"))
                .andExpect(status().isOk()).andExpect(model().attribute("tokenValid", false));
        mvc.perform(post("/auth/forgot-password").with(csrf()).param("email", "bad"))
                .andExpect(model().attributeHasFieldErrors("forgotPasswordRequest", "email"));
    }

    @Test
    void forgotPasswordRateLimitReturns429() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/auth/forgot-password").with(csrf()).param("email", "unknown@example.com"))
                    .andExpect(status().is3xxRedirection());
        }
        mvc.perform(post("/auth/forgot-password").with(csrf()).param("email", "unknown@example.com"))
                .andExpect(status().isTooManyRequests()).andExpect(header().string("Retry-After", "60"));
    }

    @Test
    void logoutRequiresPostAndCsrfAndRevokesSession() throws Exception {
        createUser();
        MockHttpSession session = login();
        mvc.perform(post("/auth/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/auth/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/auth/login?logout"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void simultaneousResetCanOnlySucceedOnce() throws Exception {
        createUser();
        String raw = issueToken();
        var start = new java.util.concurrent.CountDownLatch(1);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Boolean> redeem = () -> {
                start.await();
                try {
                    auth.resetPassword(reset(raw));
                    return true;
                } catch (InvalidResetTokenException exception) {
                    return false;
                }
            };
            var first = pool.submit(redeem);
            var second = pool.submit(redeem);
            start.countDown();
            assertThat(List.of(first.get(10, java.util.concurrent.TimeUnit.SECONDS),
                    second.get(10, java.util.concurrent.TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void adminBootstrapIsOptInAndDoesNotPromoteExistingCustomer() {
        createUser();
        var initializer = new com.busbooking.bus_ticket_management.config.AccountInitializer(
                roles, users, encoder, "admin_test", "admin@example.com", PASSWORD);
        initializer.run();
        initializer.run();
        User admin = users.findByUsername("admin_test").orElseThrow();
        assertThat(admin.getRole().getName()).isEqualTo(Role.ADMIN);
        assertThat(encoder.matches(PASSWORD, admin.getPassword())).isTrue();
        var collision = new com.busbooking.bus_ticket_management.config.AccountInitializer(
                roles, users, encoder, "minh_test", "minh@example.com", PASSWORD);
        assertThatThrownBy(collision::run).isInstanceOf(IllegalStateException.class);
        assertThat(users.findByUsername("minh_test").orElseThrow().getRole().getName())
                .isEqualTo(Role.CUSTOMER);
    }

    @Test
    void unchangedPasswordIsRejectedAndTokenRemainsUsable() {
        createUser();
        String raw = issueToken();
        ResetPasswordRequest request = reset(raw);
        request.setNewPassword(PASSWORD);
        request.setConfirmPassword(PASSWORD);
        assertThatThrownBy(() -> auth.resetPassword(request)).isInstanceOf(AccountValidationException.class);
        assertThat(auth.isResetTokenValid(raw)).isTrue();
    }

    @Test
    void oldPasswordStopsWorkingAfterReset() throws Exception {
        createUser();
        auth.resetPassword(reset(issueToken()));
        mvc.perform(post("/auth/login").with(csrf()).param("username", "minh_test")
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/auth/login?error"));
    }

    @Test
    void securityHistoryOnlyDisplaysTheCurrentUsersEvents() throws Exception {
        User owner = createUser();
        userService.register(registration("other_user", "other@example.com"));
        User other = users.findByUsername("other_user").orElseThrow();
        auditService.record(owner.getId(), SecurityEventType.PASSWORD_CHANGED);
        auditService.record(other.getId(), SecurityEventType.ALL_SESSIONS_REVOKED);
        MockHttpSession session = login();
        var response = mvc.perform(get("/user/security/history").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đổi mật khẩu")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Đăng nhập thành công")))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        // The label also appears in filter options; check actual projected data for isolation.
        var page = auditService.history(owner.getId(), null, 0);
        assertThat(page.getContent()).noneMatch(event -> event.action().equals(
                SecurityEventType.ALL_SESSIONS_REVOKED.getLabel()));
        assertThat(response).doesNotContain("other@example.com");
    }

    @Test
    void historySupportsFilteringPaginationAndEmptyState() throws Exception {
        User owner = createUser();
        for (int i = 0; i < 18; i++) auditService.record(owner.getId(), SecurityEventType.PROFILE_UPDATED);
        MockHttpSession session = login();
        assertThat(auditService.history(owner.getId(), SecurityEventType.PROFILE_UPDATED, 0).getSize()).isEqualTo(15);
        assertThat(auditService.history(owner.getId(), SecurityEventType.PROFILE_UPDATED, 1).getNumberOfElements()).isEqualTo(3);
        mvc.perform(get("/user/security/history").session(session)
                        .param("type", "PROFILE_UPDATED").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Trang 2 / 2")));
        mvc.perform(get("/user/security/history").session(session).param("type", "ALL_SESSIONS_REVOKED"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Chưa có hoạt động phù hợp")));
        mvc.perform(get("/user/security/history").session(session).param("page", "-5"))
                .andExpect(status().isOk());
    }

    @Test
    void historyDoesNotRenderInjectedUserAgentAsHtml() throws Exception {
        createUser();
        mvc.perform(post("/auth/login").with(csrf()).param("username", "minh_test")
                        .param("password", PASSWORD).header("User-Agent", "<script>alert(1)</script>"))
                .andExpect(redirectedUrl("/user/profile"));
        MockHttpSession session = login();
        mvc.perform(get("/user/security/history").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")));
    }

    @Test
    void loginSuccessAndFailureAreRecordedWithoutSecrets() throws Exception {
        User owner = createUser();
        mvc.perform(post("/auth/login").with(csrf()).param("username", "minh_test")
                        .param("password", "wrong-password"))
                .andExpect(redirectedUrl("/auth/login?error"));
        MockHttpSession session = login();
        assertThat(auditService.history(owner.getId(), SecurityEventType.LOGIN_FAILED, 0).getTotalElements()).isEqualTo(1);
        assertThat(auditService.history(owner.getId(), SecurityEventType.LOGIN_SUCCESS, 0).getTotalElements()).isEqualTo(1);
        String response = mvc.perform(get("/user/security/history").session(session))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(response).doesNotContain(PASSWORD, "wrong-password", owner.getPassword(), "JSESSIONID", AuthService.digest(session.getId()));
    }

    @Test
    void failedProfileUpdateDoesNotRecordSuccessfulChange() throws Exception {
        User owner = createUser();
        mvc.perform(post("/user/profile").session(login()).with(csrf())
                        .param("fullName", "Không được lưu").param("email", "new@example.com")
                        .param("currentPassword", "wrong"))
                .andExpect(model().attributeHasFieldErrors("updateProfileRequest", "currentPassword"));
        assertThat(auditService.history(owner.getId(), SecurityEventType.PROFILE_UPDATED, 0).getTotalElements()).isZero();
    }

    @Test
    void sessionListShowsCurrentAndOtherSessionWithoutRawCookieIds() throws Exception {
        createUser();
        MockHttpSession first = login();
        MockHttpSession second = login();
        String html = mvc.perform(get("/user/security/sessions").session(second))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Phiên hiện tại")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Phiên khác")))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).doesNotContain("value=\"" + first.getId() + "\"",
                "value=\"" + second.getId() + "\"", "JSESSIONID",
                AuthService.digest(first.getId()), AuthService.digest(second.getId()));
        assertThat(sessionRecords.findAll()).hasSize(2)
                .allMatch(record -> record.getSessionHash().length() == 64);
        var principal = new com.busbooking.bus_ticket_management.security.AccountPrincipal(users.findByUsername("minh_test").orElseThrow());
        assertThat(accountSessions.active(principal, second)).hasSize(2)
                .filteredOn(item -> item.current()).hasSize(1);
    }

    @Test
    void ownerCanRevokeSpecificOtherSession() throws Exception {
        User owner = createUser();
        MockHttpSession first = login();
        MockHttpSession second = login();
        String id = sessionRecords.findBySessionHashAndUserId(AuthService.digest(first.getId()), owner.getId())
                .orElseThrow().getId();
        mvc.perform(post("/user/security/sessions/" + id + "/revoke").session(second).with(csrf()))
                .andExpect(redirectedUrl("/user/security/sessions"));
        mvc.perform(get("/user/profile").session(first)).andExpect(redirectedUrl("/auth/login?expired"));
        mvc.perform(get("/user/profile").session(second)).andExpect(status().isOk());
        assertThat(auditService.history(owner.getId(), SecurityEventType.SESSION_REVOKED, 0).getTotalElements()).isEqualTo(1);
    }

    @Test
    void anotherUsersSessionCannotBeRevokedByGuessingItsManagementId() throws Exception {
        User owner = createUser();
        MockHttpSession ownersSession = login();
        String id = sessionRecords.findBySessionHashAndUserId(AuthService.digest(ownersSession.getId()), owner.getId())
                .orElseThrow().getId();
        userService.register(registration("other_user", "other@example.com"));
        MockHttpSession other = (MockHttpSession) mvc.perform(post("/auth/login").with(csrf())
                        .param("username", "other_user").param("password", PASSWORD))
                .andReturn().getRequest().getSession(false);
        mvc.perform(post("/user/security/sessions/" + id + "/revoke").session(other).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/user/profile").session(ownersSession)).andExpect(status().isOk());
    }

    @Test
    void revokeOthersKeepsCurrentSession() throws Exception {
        createUser();
        MockHttpSession first = login();
        MockHttpSession second = login();
        mvc.perform(post("/user/security/sessions/revoke-others").session(second).with(csrf()))
                .andExpect(redirectedUrl("/user/security/sessions"));
        mvc.perform(get("/user/profile").session(first)).andExpect(redirectedUrl("/auth/login?expired"));
        mvc.perform(get("/user/profile").session(second)).andExpect(status().isOk());
    }

    @Test
    void revokeAllLogsOutCurrentAndOtherSessions() throws Exception {
        createUser();
        MockHttpSession first = login();
        MockHttpSession second = login();
        mvc.perform(post("/user/security/sessions/revoke-all").session(second).with(csrf()))
                .andExpect(redirectedUrl("/auth/login?logout"));
        assertThat(second.isInvalid()).isTrue();
        mvc.perform(get("/user/profile").session(first)).andExpect(redirectedUrl("/auth/login?expired"));
    }

    @Test
    void revokingCurrentSessionLogsOutImmediately() throws Exception {
        User owner = createUser();
        MockHttpSession current = login();
        String id = sessionRecords.findBySessionHashAndUserId(AuthService.digest(current.getId()), owner.getId())
                .orElseThrow().getId();
        mvc.perform(post("/user/security/sessions/" + id + "/revoke").session(current).with(csrf()))
                .andExpect(redirectedUrl("/auth/login?logout"));
        assertThat(current.isInvalid()).isTrue();
    }

    @Test
    void revocationNeedsCsrfAndExpiredSessionMetadataDeniesAccess() throws Exception {
        User owner = createUser();
        MockHttpSession current = login();
        mvc.perform(post("/user/security/sessions/revoke-all").session(current)).andExpect(status().isForbidden());
        var record = sessionRecords.findBySessionHashAndUserId(AuthService.digest(current.getId()), owner.getId())
                .orElseThrow();
        record.setExpiresAt(Instant.now().minusSeconds(1));
        sessionRecords.saveAndFlush(record);
        mvc.perform(get("/user/profile").session(current)).andExpect(redirectedUrl("/auth/login?expired"));
    }

    @Test
    void fiveFailuresBlockEvenCorrectPasswordAndRemainBlockedAcrossClientIps() throws Exception {
        createUser();
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/auth/login").with(csrf()).param("username", "MINH_TEST")
                            .param("password", "incorrect"))
                    .andExpect(status().is3xxRedirection());
        }
        assertThat(loginAttempts.remainingSeconds("minh_test")).isBetween(1L, 900L);
        mvc.perform(post("/auth/login").with(csrf()).param("username", "minh_test")
                        .param("password", PASSWORD).with(request -> {
                            request.setRemoteAddr("another-client");
                            return request;
                        }))
                .andExpect(redirectedUrlPattern("/auth/login?retrySeconds=*"));
        assertThat(sessionRecords.count()).isZero();
        mvc.perform(get("/auth/login").param("retrySeconds", "900"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-lock-countdown")));
    }

    @Test
    void expiredLoginLockAllowsAuthenticationAndClearsCounter() throws Exception {
        createUser();
        for (int i = 0; i < 5; i++) loginAttempts.failed("minh_test");
        var attempt = attemptRecords.findById(AuthService.digest("minh_test")).orElseThrow();
        attempt.setLockedUntil(Instant.now().minusSeconds(1));
        attemptRecords.saveAndFlush(attempt);
        login();
        assertThat(attemptRecords.existsById(AuthService.digest("minh_test"))).isFalse();
    }

    @Test
    void successfulLoginResetsPreviousFailures() throws Exception {
        createUser();
        for (int i = 0; i < 4; i++) loginAttempts.failed("minh_test");
        login();
        assertThat(attemptRecords.count()).isZero();
    }

    @Test
    void unknownNamesUseSameLockPolicyWithoutCreatingSecurityHistory() throws Exception {
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/auth/login").with(csrf()).param("username", "unknown_account")
                            .param("password", "incorrect"))
                    .andExpect(status().is3xxRedirection());
        }
        assertThat(loginAttempts.remainingSeconds("UNKNOWN_ACCOUNT")).isPositive();
        assertThat(auditRecords.count()).isZero();
        assertThat(attemptRecords.findAll()).allMatch(attempt ->
                !attempt.getUsernameHash().contains("unknown_account"));
    }

    @Test
    void passwordResetClearsTemporaryLoginBlock() throws Exception {
        User owner = createUser();
        for (int i = 0; i < 5; i++) loginAttempts.failed("minh_test");
        auth.resetPassword(reset(issueToken()));
        assertThat(loginAttempts.remainingSeconds("minh_test")).isZero();
        mvc.perform(post("/auth/login").with(csrf()).param("username", "minh_test")
                        .param("password", NEXT_PASSWORD))
                .andExpect(redirectedUrl("/user/profile"));
        assertThat(auditService.history(owner.getId(), SecurityEventType.PASSWORD_RESET, 0).getTotalElements()).isEqualTo(1);
    }

    @Test
    void dailyMaintenanceDeletesOnlyExpiredAccountMetadata() throws Exception {
        User owner = createUser();
        MockHttpSession current = login();
        SecurityEvent oldEvent = new SecurityEvent();
        oldEvent.setUserId(owner.getId());
        oldEvent.setType(SecurityEventType.LOGIN_FAILED);
        oldEvent.setOccurredAt(Instant.now().minus(java.time.Duration.ofDays(100)));
        auditRecords.saveAndFlush(oldEvent);
        var expired = new AccountSession();
        expired.setId(UUID.randomUUID().toString());
        expired.setSessionHash(AuthService.digest("expired-test-session"));
        expired.setUserId(owner.getId());
        expired.setCreatedAt(Instant.now().minus(java.time.Duration.ofDays(3)));
        expired.setLastSeenAt(expired.getCreatedAt());
        expired.setExpiresAt(Instant.now().minus(java.time.Duration.ofDays(2)));
        sessionRecords.saveAndFlush(expired);
        loginAttempts.failed("stale_account");
        var attempt = attemptRecords.findById(AuthService.digest("stale_account")).orElseThrow();
        attempt.setWindowEndsAt(Instant.now().minus(java.time.Duration.ofDays(2)));
        attemptRecords.saveAndFlush(attempt);
        context.getBean(com.busbooking.bus_ticket_management.config.AccountMaintenanceConfig.class)
                .cleanExpiredMetadata();
        assertThat(auditRecords.existsById(oldEvent.getId())).isFalse();
        assertThat(auditRecords.count()).isPositive();
        assertThat(sessionRecords.existsById(expired.getId())).isFalse();
        assertThat(attemptRecords.count()).isZero();
        mvc.perform(get("/user/profile").session(current)).andExpect(status().isOk());
    }
}
