package com.busbooking.bus_ticket_management.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BusTripSearchCriteria {

    private String departureLocation;
    private String arrivalLocation;
    private LocalDateTime departureTimeFrom;
    private LocalDateTime departureTimeTo;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private String busType;
    private Long companyId;
    private String companyName;

    // Phân trang và sắp xếp
    private int page = 0;
    private int size = 10;
    private String sortBy = "departureTime";
    private String sortDirection = "ASC";

    public BusTripSearchCriteria() {}

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

    public LocalDateTime getDepartureTimeFrom() {
        return departureTimeFrom;
    }

    public void setDepartureTimeFrom(LocalDateTime departureTimeFrom) {
        this.departureTimeFrom = departureTimeFrom;
    }

    public LocalDateTime getDepartureTimeTo() {
        return departureTimeTo;
    }

    public void setDepartureTimeTo(LocalDateTime departureTimeTo) {
        this.departureTimeTo = departureTimeTo;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
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

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page < 0 ? 0 : page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size <= 0 ? 10 : size;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}