package com.busbooking.bus_ticket_management.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Một dòng kết quả tìm kiếm gửi ra giao diện (HTML và JSON).
 * Các chuỗi định dạng sẵn (giờ, giá...) giúp giao diện không phải tính toán lại.
 * <p>
 * Lưu ý: BusTrip chưa có giờ đến và lượt đánh giá => DTO chưa có các trường này.
 * "rating" là điểm đánh giá của NHÀ XE (BusCompany.rating).
 * "availableSeats" = tổng ghế - số ghế đang giữ/đã đặt (bảng bus_seats của Module 4).
 */
public class TripSearchResultDto {

    private Long id;
    private String busCompany;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private String departureTimeText;
    private String dateText;
    private String dayOfWeekText;
    private BigDecimal price;
    private String priceText;
    private String busType;
    private String busTypeLabel;
    private String timeSlotLabel;
    private int totalSeats;
    private int availableSeats;
    private boolean lowSeats;
    private boolean soldOut;
    private String seatsText;
    private double rating;
    private List<String> badges = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBusCompany() {
        return busCompany;
    }

    public void setBusCompany(String busCompany) {
        this.busCompany = busCompany;
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

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public void setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
    }

    public String getDepartureTimeText() {
        return departureTimeText;
    }

    public void setDepartureTimeText(String departureTimeText) {
        this.departureTimeText = departureTimeText;
    }

    public String getDateText() {
        return dateText;
    }

    public void setDateText(String dateText) {
        this.dateText = dateText;
    }

    public String getDayOfWeekText() {
        return dayOfWeekText;
    }

    public void setDayOfWeekText(String dayOfWeekText) {
        this.dayOfWeekText = dayOfWeekText;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getPriceText() {
        return priceText;
    }

    public void setPriceText(String priceText) {
        this.priceText = priceText;
    }

    public String getBusType() {
        return busType;
    }

    public void setBusType(String busType) {
        this.busType = busType;
    }

    public String getBusTypeLabel() {
        return busTypeLabel;
    }

    public void setBusTypeLabel(String busTypeLabel) {
        this.busTypeLabel = busTypeLabel;
    }

    public String getTimeSlotLabel() {
        return timeSlotLabel;
    }

    public void setTimeSlotLabel(String timeSlotLabel) {
        this.timeSlotLabel = timeSlotLabel;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }

    public boolean isLowSeats() {
        return lowSeats;
    }

    public void setLowSeats(boolean lowSeats) {
        this.lowSeats = lowSeats;
    }

    public boolean isSoldOut() {
        return soldOut;
    }

    public void setSoldOut(boolean soldOut) {
        this.soldOut = soldOut;
    }

    public String getSeatsText() {
        return seatsText;
    }

    public void setSeatsText(String seatsText) {
        this.seatsText = seatsText;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public List<String> getBadges() {
        return badges;
    }

    public void setBadges(List<String> badges) {
        this.badges = badges == null ? new ArrayList<>() : badges;
    }
}
