package com.busbooking.bus_ticket_management.exception;


/** Expired, invalid, disabled-account or previously used reset token. */
public class InvalidResetTokenException extends RuntimeException {
    private final String field;

    public InvalidResetTokenException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
