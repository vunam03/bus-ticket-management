package com.busbooking.bus_ticket_management.exception;


/** Current password could not be verified. */
public class InvalidCredentialsException extends RuntimeException {
    private final String field;

    public InvalidCredentialsException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
