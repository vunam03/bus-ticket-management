package com.busbooking.bus_ticket_management.model;
import jakarta.persistence.*;
import org.springframework.web.bind.annotation.CookieValue;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_trip")

public class BusTrip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_name", nullable = false, length = 150)
    private String routeName;

    @Column(name = "arrival_location", nullable = false,length = 100)
    private String arrivalLocation;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "ticket_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    @Column(name = "total_seats",nullable = false)
    private Integer toatalSeats;

    @Column(name = "bus_type", length = 50)
    private String busType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private BusCompany busCompany;

    public BusTrip() {
    }

    public BusTrip(Long id, String routeName, String arrivalLocation,
                   LocalDateTime departureTime, BigDecimal ticketPrice,
                   Integer toatalSeats, String busType, BusCompany busCompany) {
        this.id = id;
        this.routeName = routeName;
        this.arrivalLocation = arrivalLocation;
        this.departureTime = departureTime;
        this.ticketPrice = ticketPrice;
        this.toatalSeats = toatalSeats;
        this.busType = busType;
        this.busCompany = busCompany;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public String getArrivalLocation() {
        return arrivalLocation;
    }

    public void setArrivalLocation(String arrivalLocation) {
        this.arrivalLocation = arrivalLocation;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public BigDecimal getTicketPrice() {
        return ticketPrice;
    }

    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice;
    }

    public Integer getToatalSeats() {
        return toatalSeats;
    }

    public void setToatalSeats(Integer toatalSeats) {
        this.toatalSeats = toatalSeats;
    }

    public String getBusType() {
        return busType;
    }

    public void setBusType(String busType) {
        this.busType = busType;
    }

    public BusCompany getBusCompany() {
        return busCompany;
    }

    public void setBusCompany(BusCompany busCompany) {
        this.busCompany = busCompany;
    }


}
