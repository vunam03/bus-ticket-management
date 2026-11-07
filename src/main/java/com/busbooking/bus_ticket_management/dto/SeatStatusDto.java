package com.busbooking.bus_ticket_management.dto;

import com.busbooking.bus_ticket_management.model.Seat;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight seat status payload used by the real-time seat map API.
 */
public class SeatStatusDto {

    private Long seatId;
    private String seatNumber;
    private Integer floorNumber;
    private Integer rowIndex;
    private Integer columnIndex;
    private BigDecimal price;
    private String status;
    private boolean heldByCurrentUser;
    private LocalDateTime holdExpiresAt;

    public SeatStatusDto() {
    }

    public static SeatStatusDto fromEntity(Seat seat, boolean heldByCurrentUser, LocalDateTime holdExpiresAt) {
        SeatStatusDto dto = new SeatStatusDto();
        dto.setSeatId(seat.getId());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setFloorNumber(seat.getFloorNumber());
        dto.setRowIndex(seat.getRowIndex());
        dto.setColumnIndex(seat.getColumnIndex());
        dto.setPrice(seat.getPrice());
        dto.setStatus(seat.getSeatStatus() != null ? seat.getSeatStatus().name() : Seat.SeatStatus.AVAILABLE.name());
        dto.setHeldByCurrentUser(heldByCurrentUser);
        dto.setHoldExpiresAt(holdExpiresAt);
        return dto;
    }

    public Long getSeatId() {
        return seatId;
    }

    public void setSeatId(Long seatId) {
        this.seatId = seatId;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Integer getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(Integer floorNumber) {
        this.floorNumber = floorNumber;
    }

    public Integer getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }

    public Integer getColumnIndex() {
        return columnIndex;
    }

    public void setColumnIndex(Integer columnIndex) {
        this.columnIndex = columnIndex;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHeldByCurrentUser() {
        return heldByCurrentUser;
    }

    public void setHeldByCurrentUser(boolean heldByCurrentUser) {
        this.heldByCurrentUser = heldByCurrentUser;
    }

    public LocalDateTime getHoldExpiresAt() {
        return holdExpiresAt;
    }

    public void setHoldExpiresAt(LocalDateTime holdExpiresAt) {
        this.holdExpiresAt = holdExpiresAt;
    }
}
