package com.busbooking.bus_ticket_management.exception;


/** A domain validation failure associated with a form field. */
public class AccountValidationException extends RuntimeException {
    private final String field;

    public AccountValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
