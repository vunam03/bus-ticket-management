package com.busbooking.bus_ticket_management.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bus_companies")

public class BusCompany {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "rating")
    private double rating;

    @OneToMany(mappedBy = "busCompany", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<BusTrip> busTrips = new ArrayList<>();

    public BusCompany() {
    }

    public BusCompany(Long id, String name, String phoneNumber, String address,
                      double rating, List<BusTrip> busTrips) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.rating = rating;
        this.busTrips = busTrips;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public List<BusTrip> getBusTrips() {
        return busTrips;
    }

    public void setBusTrips(List<BusTrip> busTrips) {
        this.busTrips = busTrips;
    }
}
