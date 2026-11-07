package com.busbooking.bus_ticket_management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;


public class BusTripRequest {
    @NotBlank(message = "Tên tuyến đường không được để trống")
    private String routeName;

    @NotBlank(message = "Điểm đi không được để trống")
    private String departureLocation;

    @NotBlank(message = "Điểm đến không được để trống")
    private String arivalLocation;

    @NotNull(message = "Thời gian xuất bến không dược để trống")
    @Future(message = "Thời gian xuất bến phải là thời gian tương lai")
    private LocalDateTime departureTime;

    @NotNull(message = "Giá vé không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá vé phải lớn hơn không")
    private BigDecimal ticketPrice;

    @NotNull(message = "Tổng số ghế không được để trống")
    private Integer totalSeats;

    private String busType;

    @NotNull(message = "ID nhà xe không được để trống")
    private Long companyId;

    public BusTripRequest() {
    }

    public BusTripRequest(String routeName, String departureLocation,
                          String arivalLocation, LocalDateTime departureTime,
                          BigDecimal ticketPrice, Integer totalSeats, String busType,
                          Long companyId) {
        this.routeName = routeName;
        this.departureLocation = departureLocation;
        this.arivalLocation = arivalLocation;
        this.departureTime = departureTime;
        this.ticketPrice = ticketPrice;
        this.totalSeats = totalSeats;
        this.busType = busType;
        this.companyId = companyId;
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

    public String getArivalLocation() {
        return arivalLocation;
    }

    public void setArivalLocation(String arivalLocation) {
        this.arivalLocation = arivalLocation;
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


}
