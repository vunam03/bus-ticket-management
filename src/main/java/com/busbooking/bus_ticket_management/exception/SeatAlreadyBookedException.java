package com.busbooking.bus_ticket_management.exception;

/**
 * Thrown when a seat cannot be selected because it is already booked or
 * is currently held by another customer.
 */
public class SeatAlreadyBookedException extends RuntimeException {

    private final Long seatId;
    private final String seatNumber;

    public SeatAlreadyBookedException(String message) {
        super(message);
        this.seatId = null;
        this.seatNumber = null;
    }

    public SeatAlreadyBookedException(Long seatId, String seatNumber, String message) {
        super(message);
        this.seatId = seatId;
        this.seatNumber = seatNumber;
    }

    public Long getSeatId() {
        return seatId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }
}
