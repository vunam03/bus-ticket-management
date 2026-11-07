package com.busbooking.bus_ticket_management.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_trips")
public class BusTrip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "route_name", nullable = false, length = 150)
    private String routeName;

    @Column(name = "departure_location", nullable = false, length = 100)
    private String departureLocation;

    @Column(name = "arrival_location", nullable = false, length = 100)
    private String arrivalLocation;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "ticket_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal ticketPrice;

    @Column(name = "total_seats", nullable = false)
    private Integer totalSeats;

    @Column(name = "floor_count", nullable = false)
    private Integer floorCount = 2;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "bus_type", length = 50)
    private String busType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private BusCompany busCompany;

    public BusTrip() {}

    public BusTrip(Long id, String routeName, String departureLocation, String arrivalLocation,
                   LocalDateTime departureTime, BigDecimal ticketPrice, Integer totalSeats,
                   Integer floorCount, BigDecimal basePrice, String busType, BusCompany busCompany) {
        this.id = id;
        this.routeName = routeName;
        this.departureLocation = departureLocation;
        this.arrivalLocation = arrivalLocation;
        this.departureTime = departureTime;
        this.ticketPrice = ticketPrice;
        this.totalSeats = totalSeats;
        this.floorCount = floorCount;
        this.basePrice = basePrice;
        this.busType = busType;
        this.busCompany = busCompany;
    }

    public Long getId() {
        return id; }
    public void setId(Long id) {
        this.id = id; }
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
    public Integer getFloorCount() {
        return floorCount; }
    public void setFloorCount(Integer floorCount) {
        this.floorCount = floorCount; }
    public BigDecimal getBasePrice() {
        return basePrice; }
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice; }
    public String getBusType() {
        return busType; }
    public void setBusType(String busType) {
        this.busType = busType; }
    public BusCompany getBusCompany() {
        return busCompany; }
    public void setBusCompany(BusCompany busCompany) {
        this.busCompany = busCompany; }
}