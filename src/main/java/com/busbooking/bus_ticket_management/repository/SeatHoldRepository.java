package com.busbooking.bus_ticket_management.repository;

import com.busbooking.bus_ticket_management.model.SeatHold;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Data access for temporary seat holds, including expiration-based cleanup queries.
 */
public interface SeatHoldRepository extends JpaRepository<SeatHold, Long> {

    Optional<SeatHold> findBySeatId(Long seatId);

    List<SeatHold> findByBusTripId(Long tripId);

    List<SeatHold> findByBusTripIdAndHolderKey(Long tripId, String holderKey);

    List<SeatHold> findByHoldExpiresAtBefore(LocalDateTime cutoff);

    @Query("select h from SeatHold h join fetch h.seat where h.holdExpiresAt <= :now")
    List<SeatHold> findExpiredHoldsWithSeats(@Param("now") LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SeatHold h where h.id in :ids")
    int deleteByIds(@Param("ids") List<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from SeatHold h where h.seat.id = :seatId")
    int deleteBySeatId(@Param("seatId") Long seatId);
    @Query("select h.seat.id from SeatHold h where h.holdExpiresAt <= :now order by h.seat.id")
    List<Long> findExpiredSeatIds(@Param("now") LocalDateTime now);
}
