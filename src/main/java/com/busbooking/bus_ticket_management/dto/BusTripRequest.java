package com.busbooking.bus_ticket_management.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BusTripRequest {

    @NotBlank(message = "Tên tuyến đường không được để trống")
    private String routeName;

    @NotBlank(message = "Điểm đi không được để trống")
    private String departureLocation;

    @NotBlank(message = "Điểm đến không được để trống")
    private String arrivalLocation;

    @NotNull(message = "Thời gian xuất bến không được để trống")
    @Future(message = "Thời gian xuất bến phải là thời gian trong tương lai")
    private LocalDateTime departureTime;

    @NotNull(message = "Giá vé không được để trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá vé phải lớn hơn 0")
    private BigDecimal ticketPrice;

    @NotNull(message = "Tổng số ghế không được để trống")
    private Integer totalSeats;

    private String busType;

    @NotNull(message = "ID nhà xe không được để trống")
    private Long companyId;

    public BusTripRequest() {}

    public String getRouteName() {
        return routeName; }
    public void setRouteName(String routeName) {
        this.routeName = routeName; }
    public String getDepartureLocation() {
        return departureLocation; }
    public void setDepartureLocation(String departureLocation) {
        this.departureLocation = departureLocation; }
    public String getArrivalLocation() {
        return arrivalLocation; }
    public void setArrivalLocation(String arrivalLocation) {
        this.arrivalLocation = arrivalLocation; }
    public LocalDateTime getDepartureTime() {
        return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime; }
    public BigDecimal getTicketPrice() {
        return ticketPrice; }
    public void setTicketPrice(BigDecimal ticketPrice) {
        this.ticketPrice = ticketPrice; }
    public Integer getTotalSeats() {
        return totalSeats; }
    public void setTotalSeats(Integer totalSeats) {
        this.totalSeats = totalSeats; }
    public String getBusType() {
        return busType; }
    public void setBusType(String busType) {
        this.busType = busType; }
    public Long getCompanyId() {
        return companyId; }
    public void setCompanyId(Long companyId) {
        this.companyId = companyId; }
}