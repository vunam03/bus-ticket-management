package com.busbooking.bus_ticket_management.repository;

import com.busbooking.bus_ticket_management.model.BusTrip;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for loading bus trips associated with seat maps.
 */
public interface BusTripRepository extends JpaRepository<BusTrip, Long> {
}
