package com.busbooking.bus_ticket_management.repository;

import com.busbooking.bus_ticket_management.model.Seat;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for trip seats. Critical mutations use pessimistic row locks
 * to prevent two customers from holding or booking the same seat concurrently.
 */
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByBusTripIdOrderByFloorNumberAscRowIndexAscColumnIndexAscSeatNumberAsc(Long tripId);

    Optional<Seat> findByIdAndBusTripId(Long seatId, Long tripId);

    /**
     * Loads a seat with a pessimistic write lock. Callers must run inside a transaction.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id = :seatId and s.busTrip.id = :tripId")
    Optional<Seat> findByIdAndTripIdForUpdate(@Param("seatId") Long seatId, @Param("tripId") Long tripId);

    long countByBusTripId(Long tripId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Seat s set s.seatStatus = :available " +
           "where s.id in :seatIds and s.seatStatus = :hold")
    int releaseHeldSeats(@Param("seatIds") List<Long> seatIds,
                         @Param("available") Seat.SeatStatus available,
                         @Param("hold") Seat.SeatStatus hold);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Seat s where s.id = :id")
    Optional<Seat> findSeatForUpdate(@Param("id") Long id);
}
