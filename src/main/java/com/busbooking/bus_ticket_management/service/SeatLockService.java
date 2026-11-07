package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.SeatLayoutResponse;
import com.busbooking.bus_ticket_management.dto.SeatStatusDto;

/**
 * Service responsible for temporary seat holds, conflict prevention, and real-time status.
 */
public interface SeatLockService {

    int HOLD_MINUTES = 10;

    /**
     * Attempts to place a temporary hold on a seat for the given holder.
     * Uses pessimistic locking to prevent concurrent double-holds.
     *
     * @return updated status for the affected seat
     */
    SeatStatusDto holdSeat(Long tripId, Long seatId, String holderKey);

    /**
     * Releases a seat previously held by the given holder.
     *
     * @return updated status for the affected seat
     */
    SeatStatusDto releaseSeat(Long tripId, Long seatId, String holderKey);

    /**
     * Builds the current seat layout for a trip, including which seats are held by holderKey.
     */
    SeatLayoutResponse getSeatLayout(Long tripId, String holderKey);

    /**
     * Releases all expired holds. Intended for scheduled execution and on-demand cleanup.
     *
     * @return number of holds released
     */
    int releaseExpiredHolds();
}
