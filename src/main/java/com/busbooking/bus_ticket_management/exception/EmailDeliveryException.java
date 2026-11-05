package com.busbooking.bus_ticket_management.exception;


/** A recoverable form error; never expose SMTP details to the user. */
public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException() {
        super("Chưa gửi được email xác nhận. Vui lòng thử lại sau.");
    }
}
