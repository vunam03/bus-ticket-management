package com.busbooking.bus_ticket_management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.math.BigDecimal;

/**
 * Represents a single physical seat on a specific bus trip.
 * Status transitions are controlled by the seat lock service to prevent
 * concurrent users from holding or booking the same seat.
 */
@Entity
@Table(
    name = "bus_seats",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_bus_seats_trip_seat", columnNames = {"trip_id", "seat_number"})
    },
    indexes = {
        @Index(name = "idx_bus_seats_trip_status", columnList = "trip_id, seat_status"),
        @Index(name = "idx_bus_seats_trip_floor", columnList = "trip_id, floor_number")
    }
)
public class Seat {

    public enum SeatStatus {
        AVAILABLE,
        HOLD,
        BOOKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", length = 20)
    private SeatType seatType = SeatType.NORMAL;
    @Column(name = "seat_benefits", length = 500)
    private String benefits;
    public String getBenefits() { return benefits == null || benefits.isBlank() ? "Tiện ích theo thông tin nhà xe; vui lòng xác nhận trước khi đặt." : benefits; }
    public void setBenefits(String value) { benefits = value; }
    public SeatType getSeatType() { return seatType == null ? SeatType.NORMAL : seatType; }
    public void setSeatType(SeatType value) { seatType = value == null ? SeatType.NORMAL : value; }


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private BusTrip busTrip;

    @Column(name = "seat_number", nullable = false, length = 10)
    private String seatNumber;

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;

    @Column(name = "row_index", nullable = false)
    private Integer rowIndex;

    @Column(name = "column_index", nullable = false)
    private Integer columnIndex;

    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_status", nullable = false, length = 20)
    private SeatStatus seatStatus = SeatStatus.AVAILABLE;

    /**
     * Optimistic lock version used as an additional safeguard for status updates.
     * Pessimistic locks are still used in critical hold/release operations.
     */
    @Version
    @Column(name = "version")
    private Long version;

    public Seat() {
    }

    public Seat(BusTrip busTrip, String seatNumber, Integer floorNumber, Integer rowIndex, Integer columnIndex, BigDecimal price) {
        this.busTrip = busTrip;
        this.seatNumber = seatNumber;
        this.floorNumber = floorNumber;
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.price = price;
        this.seatStatus = SeatStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BusTrip getBusTrip() {
        return busTrip;
    }

    public void setBusTrip(BusTrip busTrip) {
        this.busTrip = busTrip;
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

    public Seat.SeatStatus getSeatStatus() {
        return seatStatus;
    }

    public void setSeatStatus(Seat.SeatStatus seatStatus) {
        this.seatStatus = seatStatus;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
