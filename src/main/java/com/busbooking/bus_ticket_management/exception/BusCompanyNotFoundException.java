package com.busbooking.bus_ticket_management.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class BusCompanyNotFoundException extends RuntimeException {
    public BusCompanyNotFoundException(String message) {
        super(message);
    }
}