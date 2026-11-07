package com.busbooking.bus_ticket_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Bus Ticket Management application.
 * Scheduling is enabled to support automatic release of expired seat holds.
 */
@SpringBootApplication
@EnableScheduling
public class BusTicketManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(BusTicketManagementApplication.class, args);
    }

}
