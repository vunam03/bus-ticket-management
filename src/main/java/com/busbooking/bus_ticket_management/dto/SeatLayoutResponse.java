package com.busbooking.bus_ticket_management.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Response payload containing the full 2-floor seat layout for a trip.
 * Seats are grouped by floor so the UI can render each deck independently.
 */
public class SeatLayoutResponse {

    private Long tripId;
    private String routeName;
    private Integer totalSeats;
    private Integer floorCount;
    private long availableCount;
    private long holdCount;
    private long bookedCount;
    private Map<Integer, List<SeatStatusDto>> floors = new LinkedHashMap<>();
    private List<SeatStatusDto> selectedSeats = new ArrayList<>();
    private BigDecimal totalSelectedPrice = BigDecimal.ZERO;
    private Long holdSecondsRemaining;

    public SeatLayoutResponse() {
    }

    public Long getTripId() {
        return tripId;
    }

    public void setTripId(Long tripId) {
        this.tripId = tripId;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public Integer getFloorCount() {
        return floorCount;
    }

    public void setFloorCount(Integer floorCount) {
        this.floorCount = floorCount;
    }

    public long getAvailableCount() {
        return availableCount;
    }

    public void setAvailableCount(long availableCount) {
        this.availableCount = availableCount;
    }

    public long getHoldCount() {
        return holdCount;
    }

    public void setHoldCount(long holdCount) {
        this.holdCount = holdCount;
    }

    public long getBookedCount() {
        return bookedCount;
    }

    public void setBookedCount(long bookedCount) {
        this.bookedCount = bookedCount;
    }

    public Map<Integer, List<SeatStatusDto>> getFloors() {
        return floors;
    }

    public void setFloors(Map<Integer, List<SeatStatusDto>> floors) {
        this.floors = floors;
    }

    public List<SeatStatusDto> getSelectedSeats() {
        return selectedSeats;
    }

    public void setSelectedSeats(List<SeatStatusDto> selectedSeats) {
        this.selectedSeats = selectedSeats;
    }

    public BigDecimal getTotalSelectedPrice() {
        return totalSelectedPrice;
    }

    public void setTotalSelectedPrice(BigDecimal totalSelectedPrice) {
        this.totalSelectedPrice = totalSelectedPrice;
    }

    public Long getHoldSecondsRemaining() {
        return holdSecondsRemaining;
    }

    public void setHoldSecondsRemaining(Long holdSecondsRemaining) {
        this.holdSecondsRemaining = holdSecondsRemaining;
    }
}
