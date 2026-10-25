package com.busbooking.bus_ticket_management.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BusTripResponse {

    private Long id;
    private String routeName;
    private String departureLocation;
    private String arrivalLocation;
    private LocalDateTime departureTime;
    private BigDecimal ticketPrice;
    private Integer totalSeats;
    private String busType;
    private Long companyId;
    private String companyName;

    public BusTripResponse() {
    }

    public BusTripResponse(Long id, String routeName, String departureLocation, String arrivalLocation,
                           LocalDateTime departureTime, BigDecimal ticketPrice, Integer totalSeats,
                           String busType, Long companyId, String companyName) {
        this.id = id;
        this.routeName = routeName;
        this.departureLocation = departureLocation;
        this.arrivalLocation = arrivalLocation;
        this.departureTime = departureTime;
        this.ticketPrice = ticketPrice;
        this.totalSeats = totalSeats;
        this.busType = busType;
        this.companyId = companyId;
        this.companyName = companyName;
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

    public String getDepartureLocation() {
        return departureLocation;
    }

    public void setDepartureLocation(String departureLocation) {
        this.departureLocation = departureLocation;
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

    public Integer getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getBusType() {
        return busType;
    }

    public void setBusType(String busType) {
        this.busType = busType;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
}