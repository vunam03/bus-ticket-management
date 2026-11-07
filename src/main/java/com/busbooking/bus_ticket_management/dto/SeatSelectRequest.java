package com.busbooking.bus_ticket_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for selecting (holding) or releasing a seat.
 * The holderKey is resolved server-side from the HTTP session when absent.
 */
public class SeatSelectRequest {

    @NotNull(message = "Trip id is required")
    private Long tripId;

    @NotNull(message = "Seat id is required")
    private Long seatId;

    /**
     * Optional explicit holder key (for API clients). When not provided,
     * the controller uses the current HTTP session id.
     */
    private String holderKey;

    /**
     * Action to perform: HOLD or RELEASE.
     */
    @NotBlank(message = "Action is required")
    private String action;

    public SeatSelectRequest() {
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getHolderKey() {
        return holderKey;
    }

    public void setHolderKey(String holderKey) {
        this.holderKey = holderKey;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
