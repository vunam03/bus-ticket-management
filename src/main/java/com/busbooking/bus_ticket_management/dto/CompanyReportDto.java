package com.busbooking.bus_ticket_management.dto;

import java.math.BigDecimal;

public class CompanyReportDto {

    private Long companyId;
    private String companyName;
    private String phoneNumber;
    private int totalTrips;
    private int totalSeatsOffered;
    private BigDecimal minTicketPrice;
    private BigDecimal maxTicketPrice;
    private BigDecimal averageTicketPrice;

    public CompanyReportDto() {}

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

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public int getTotalTrips() {
        return totalTrips;
    }

    public void setTotalTrips(int totalTrips) {
        this.totalTrips = totalTrips;
    }

    public int getTotalSeatsOffered() {
        return totalSeatsOffered;
    }

    public void setTotalSeatsOffered(int totalSeatsOffered) {
        this.totalSeatsOffered = totalSeatsOffered;
    }

    public BigDecimal getMinTicketPrice() {
        return minTicketPrice;
    }

    public void setMinTicketPrice(BigDecimal minTicketPrice) {
        this.minTicketPrice = minTicketPrice;
    }

    public BigDecimal getMaxTicketPrice() {
        return maxTicketPrice;
    }

    public void setMaxTicketPrice(BigDecimal maxTicketPrice) {
        this.maxTicketPrice = maxTicketPrice;
    }

    public BigDecimal getAverageTicketPrice() {
        return averageTicketPrice;
    }

    public void setAverageTicketPrice(BigDecimal averageTicketPrice) {
        this.averageTicketPrice = averageTicketPrice;
    }
}