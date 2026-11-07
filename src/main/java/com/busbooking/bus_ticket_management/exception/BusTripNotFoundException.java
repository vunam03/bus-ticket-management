package com.busbooking.bus_ticket_management.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BusTripNotFoundException extends RuntimeException {
    public BusTripNotFoundException(String message) {
        super(message);
    }
}