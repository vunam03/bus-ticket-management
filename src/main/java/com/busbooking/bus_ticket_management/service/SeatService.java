package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.model.BusTrip;
import java.util.List;

/**
 * Service responsible for generating and managing the physical seat layout for a trip.
 */
public interface SeatService {

    /**
     * Generates seats for the given trip when none exist yet.
     * Supports common coach configurations such as 36, 40, and 45 seats.
     *
     * @param trip the trip that owns the seats
     * @return the number of seats created
     */
    int initializeSeatsForTrip(BusTrip trip);

    /**
     * Ensures a trip has an initialized seat layout. Safe to call repeatedly.
     */
    void ensureSeatsInitialized(Long tripId);

    /**
     * Supported seat capacities for automatic layout generation.
     */
    List<Integer> getSupportedCapacities();
}
