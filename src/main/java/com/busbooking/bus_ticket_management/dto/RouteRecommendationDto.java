package com.busbooking.bus_ticket_management.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Thông tin tổng hợp của một tuyến đường (Hà Nội → Hải Phòng...).
 * Dùng cho: thẻ "Tuyến phổ biến" ở trang chủ, danh sách tuyến liên quan, và trang chi tiết tuyến.
 * Các trường chỉ có ở trang chi tiết (upcomingTrips, dailyPrices, busTypeLabels...) sẽ rỗng ở danh sách.
 */
public class RouteRecommendationDto {

    private String origin;
    private String destination;
    private long tripCount;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private BigDecimal avgPrice;
    private String minPriceText;
    private String maxPriceText;
    private String avgPriceText;
    private double avgRating;
    private long totalSeats;
    private String highlight;
    private List<String> busTypeLabels = new ArrayList<>();
    private List<String> companies = new ArrayList<>();
    private List<TripSearchResultDto> upcomingTrips = new ArrayList<>();
    private List<SearchResponseDto.DailyPrice> dailyPrices = new ArrayList<>();

    public String getRouteTitle() {
        return origin + " → " + destination;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public long getTripCount() {
        return tripCount;
    }

    public void setTripCount(long tripCount) {
        this.tripCount = tripCount;
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

    public BigDecimal getAvgPrice() {
        return avgPrice;
    }

    public void setAvgPrice(BigDecimal avgPrice) {
        this.avgPrice = avgPrice;
    }

    public String getMinPriceText() {
        return minPriceText;
    }

    public void setMinPriceText(String minPriceText) {
        this.minPriceText = minPriceText;
    }

    public String getMaxPriceText() {
        return maxPriceText;
    }

    public void setMaxPriceText(String maxPriceText) {
        this.maxPriceText = maxPriceText;
    }

    public String getAvgPriceText() {
        return avgPriceText;
    }

    public void setAvgPriceText(String avgPriceText) {
        this.avgPriceText = avgPriceText;
    }

    public double getAvgRating() {
        return avgRating;
    }

    public void setAvgRating(double avgRating) {
        this.avgRating = avgRating;
    }

    public long getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(long totalSeats) {
        this.totalSeats = totalSeats;
    }

    public String getHighlight() {
        return highlight;
    }

    public void setHighlight(String highlight) {
        this.highlight = highlight;
    }

    public List<String> getBusTypeLabels() {
        return busTypeLabels;
    }

    public void setBusTypeLabels(List<String> busTypeLabels) {
        this.busTypeLabels = busTypeLabels;
    }

    public List<String> getCompanies() {
        return companies;
    }

    public void setCompanies(List<String> companies) {
        this.companies = companies;
    }

    public List<TripSearchResultDto> getUpcomingTrips() {
        return upcomingTrips;
    }

    public void setUpcomingTrips(List<TripSearchResultDto> upcomingTrips) {
        this.upcomingTrips = upcomingTrips;
    }

    public List<SearchResponseDto.DailyPrice> getDailyPrices() {
        return dailyPrices;
    }

    public void setDailyPrices(List<SearchResponseDto.DailyPrice> dailyPrices) {
        this.dailyPrices = dailyPrices;
    }
}
