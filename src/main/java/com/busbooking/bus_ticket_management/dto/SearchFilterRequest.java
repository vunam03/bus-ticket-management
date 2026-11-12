package com.busbooking.bus_ticket_management.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Dữ liệu người dùng gửi lên khi tìm kiếm/lọc (Spring tự gắn từ query string của URL).
 * <p>
 * Ví dụ: /search/filter?origin=Hà Nội&destination=Hải Phòng&travelDate=2026-11-09
 *        &minPrice=100000&maxPrice=300000&busTypes=LIMOUSINE&busTypes=GHE_NGOI
 *        &timeSlots=MORNING&minRating=4&sortBy=RATING_DESC&page=1&size=10
 * <p>
 * Lớp này CHỈ chứa dữ liệu thô. Việc kiểm tra hợp lệ và chuẩn hoá do FilterService đảm nhiệm.
 */
public class SearchFilterRequest {

    private String origin;
    private String destination;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate travelDate;

    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private List<String> busTypes = new ArrayList<>();
    private List<String> timeSlots = new ArrayList<>();
    private Double minRating;
    private String sortBy = SortOption.DEFAULT.name();
    private Integer page = 1;
    private Integer size = 10;

    public SearchFilterRequest() {
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

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
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

    public List<String> getBusTypes() {
        return busTypes;
    }

    public void setBusTypes(List<String> busTypes) {
        this.busTypes = busTypes == null ? new ArrayList<>() : busTypes;
    }

    public List<String> getTimeSlots() {
        return timeSlots;
    }

    public void setTimeSlots(List<String> timeSlots) {
        this.timeSlots = timeSlots == null ? new ArrayList<>() : timeSlots;
    }

    public Double getMinRating() {
        return minRating;
    }

    public void setMinRating(Double minRating) {
        this.minRating = minRating;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "SearchFilterRequest{origin='" + origin + "', destination='" + destination
                + "', travelDate=" + travelDate
                + ", price=[" + minPrice + ", " + maxPrice + "], busTypes=" + busTypes
                + ", timeSlots=" + timeSlots + ", minRating=" + minRating
                + ", sortBy=" + sortBy + ", page=" + page + ", size=" + size + "}";
    }
}
