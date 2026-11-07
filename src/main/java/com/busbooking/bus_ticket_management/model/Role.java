package com.busbooking.bus_ticket_management.model;


import jakarta.persistence.*;

/** Shared authority. Registration always assigns CUSTOMER. */
@Entity
@Table(name = "roles")
public class Role {
    public static final String ADMIN = "ADMIN";
    public static final String CUSTOMER = "CUSTOMER";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    public Role() {
    }

    public Role(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
