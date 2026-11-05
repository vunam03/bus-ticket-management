package com.busbooking.bus_ticket_management.validator;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.length() < 8 || value.length() > 64
                || value.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;
        for (int character : value.codePoints().toArray()) {
            if (Character.isWhitespace(character) || Character.isSpaceChar(character) || character == 0xFEFF) {
                return false;
            }
            upper |= Character.isUpperCase(character);
            lower |= Character.isLowerCase(character);
            digit |= Character.isDigit(character);
            special |= !Character.isLetterOrDigit(character);
        }
        return upper && lower && digit && special;
    }
}
