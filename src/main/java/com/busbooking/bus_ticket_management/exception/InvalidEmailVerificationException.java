package com.busbooking.bus_ticket_management.exception;


public class InvalidEmailVerificationException extends RuntimeException {
    public InvalidEmailVerificationException(String message) {
        super(message);
    }
}
