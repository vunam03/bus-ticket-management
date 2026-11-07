package com.busbooking.bus_ticket_management.repository;

import com.busbooking.bus_ticket_management.model.BusTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusTripRepository extends JpaRepository<BusTrip, Long> {

    @Query("SELECT t FROM BusTrip t WHERE LOWER(t.departureLocation) LIKE LOWER(CONCAT('%', :departure, '%')) " +
            "AND LOWER(t.arrivalLocation) LIKE LOWER(CONCAT('%', :arrival, '%'))")
    List<BusTrip> searchTrips(@Param("departure") String departure, @Param("arrival") String arrival);

    List<BusTrip> findByBusCompanyId(Long companyId);
}