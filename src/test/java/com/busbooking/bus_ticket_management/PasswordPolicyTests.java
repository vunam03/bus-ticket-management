package com.busbooking.bus_ticket_management;

import com.busbooking.bus_ticket_management.validator.StrongPasswordValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTests {
    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"short", "abcdefgh", "ABCDEFGH", "12345678", "StrongPass1",
            "strongpass1!", "STRONGPASS1!", "Strong Pass1!", "StrongPass!"})
    void rejectsMissingPasswordRequirements(String password) {
        assertThat(validator.isValid(password, null)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"StrongPass1!", "KhauManh2@", "MậtKhẩu123!"})
    void acceptsStrongPasswords(String password) {
        assertThat(validator.isValid(password, null)).isTrue();
    }

    @Test
    void rejectsPasswordsExceedingBcryptByteLimit() {
        assertThat(validator.isValid("Á".repeat(34) + "a1!B", null)).isTrue();
        assertThat(validator.isValid("Á".repeat(35) + "a1!B", null)).isFalse();
    }

    @Test
    void rejectsPasswordsExceedingCharacterLimit() {
        assertThat(validator.isValid("Aa1!" + "b".repeat(61), null)).isFalse();
    }
}
