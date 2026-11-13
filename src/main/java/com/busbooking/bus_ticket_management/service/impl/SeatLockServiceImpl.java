package com.busbooking.bus_ticket_management.service.impl;

import com.busbooking.bus_ticket_management.dto.SeatLayoutResponse;
import com.busbooking.bus_ticket_management.dto.SeatStatusDto;
import com.busbooking.bus_ticket_management.exception.SeatAlreadyBookedException;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.model.Seat;
import com.busbooking.bus_ticket_management.model.SeatHold;
import com.busbooking.bus_ticket_management.repository.BusTripRepository;
import com.busbooking.bus_ticket_management.repository.SeatHoldRepository;
import com.busbooking.bus_ticket_management.repository.SeatRepository;
import com.busbooking.bus_ticket_management.service.SeatLockService;
import com.busbooking.bus_ticket_management.service.SeatService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements temporary seat holding with a 10-minute expiration window.
 * Concurrency is handled via pessimistic row locks on seats and transactional
 * checks to ensure a seat cannot be held/booked by two customers at once.
 */
@Service
public class SeatLockServiceImpl implements SeatLockService {

    private static final Logger log = LoggerFactory.getLogger(SeatLockServiceImpl.class);

    private final Clock clock;
    private final SeatRepository seatRepository;
    private final SeatHoldRepository seatHoldRepository;
    private final BusTripRepository busTripRepository;
    private final SeatService seatService;

    public SeatLockServiceImpl(
            SeatRepository seatRepository,
            SeatHoldRepository seatHoldRepository,
            BusTripRepository busTripRepository,
            SeatService seatService, Clock clock) {
        this.clock = clock;
        this.seatRepository = seatRepository;
        this.seatHoldRepository = seatHoldRepository;
        this.busTripRepository = busTripRepository;
        this.seatService = seatService;
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public SeatStatusDto holdSeat(Long tripId, Long seatId, String holderKey) {
        validateHolderKey(holderKey);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));

        Seat seat = seatRepository.findByIdAndTripIdForUpdate(seatId, tripId)
            .orElseThrow(() -> new IllegalArgumentException("Seat not found for trip"));

        // Release expired hold on this seat on-demand so stale rows do not block selection.
        SeatHold existingHold = seatHoldRepository.findBySeatId(seatId).orElse(null);
        if (existingHold != null && existingHold.isExpired(now)) {
            seatHoldRepository.delete(existingHold);
            seatHoldRepository.flush();
            existingHold = null;
            if (seat.getSeatStatus() == Seat.SeatStatus.HOLD) {
                seat.setSeatStatus(Seat.SeatStatus.AVAILABLE);
            }
        }

        if (seat.getSeatStatus() == Seat.SeatStatus.BOOKED) {
            throw new SeatAlreadyBookedException(seat.getId(), seat.getSeatNumber(),
                "Ghế " + seat.getSeatNumber() + " đã được bán.");
        }

        if (existingHold != null && !Objects.equals(existingHold.getHolderKey(), holderKey)) {
            throw new SeatAlreadyBookedException(seat.getId(), seat.getSeatNumber(),
                "Ghế " + seat.getSeatNumber() + " đang được giữ bởi khách hàng khác.");
        }

        LocalDateTime expiresAt = now.plusMinutes(HOLD_MINUTES);
        if (existingHold == null) {
            existingHold = new SeatHold(seat, seat.getBusTrip(), holderKey, expiresAt, now);
        } else {
            existingHold.setHoldExpiresAt(expiresAt); // extend the same holder's selection window
        }
        seat.setSeatStatus(Seat.SeatStatus.HOLD);
        seatRepository.save(seat);
        seatHoldRepository.save(existingHold);

        log.info("Seat {} held by {} on trip {} until {}", seatId, maskHolder(holderKey), tripId, expiresAt);
        return SeatStatusDto.fromEntity(seat, true, expiresAt);
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public SeatStatusDto releaseSeat(Long tripId, Long seatId, String holderKey) {
        validateHolderKey(holderKey);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));

        Seat seat = seatRepository.findByIdAndTripIdForUpdate(seatId, tripId)
            .orElseThrow(() -> new IllegalArgumentException("Seat not found for trip"));

        SeatHold hold = seatHoldRepository.findBySeatId(seatId).orElse(null);
        if (hold == null) {
            if (seat.getSeatStatus() == Seat.SeatStatus.HOLD) {
                seat.setSeatStatus(Seat.SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            }
            return SeatStatusDto.fromEntity(seat, false, null);
        }

        if (!Objects.equals(hold.getHolderKey(), holderKey)) {
            if (hold.isExpired(now)) {
                seatHoldRepository.delete(hold);
                seat.setSeatStatus(Seat.SeatStatus.AVAILABLE);
                seatRepository.save(seat);
                return SeatStatusDto.fromEntity(seat, false, null);
            }
            throw new SeatAlreadyBookedException(seat.getId(), seat.getSeatNumber(),
                "Bạn không thể nhả ghế đang được giữ bởi khách hàng khác.");
        }

        seatHoldRepository.delete(hold);
        if (seat.getSeatStatus() == Seat.SeatStatus.HOLD) {
            seat.setSeatStatus(Seat.SeatStatus.AVAILABLE);
            seatRepository.save(seat);
        }
        log.info("Seat {} released by {} on trip {}", seatId, maskHolder(holderKey), tripId);
        return SeatStatusDto.fromEntity(seat, false, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SeatLayoutResponse getSeatLayout(Long tripId, String holderKey) {
        seatService.ensureSeatsInitialized(tripId);
        // Read-only view applies expiration checks for correctness in responses.
        // Actual cleanup of stale rows is handled by releaseExpiredHolds()/on-demand mutations.
        BusTrip trip = busTripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        List<Seat> seats = seatRepository.findByBusTripIdOrderByFloorNumberAscRowIndexAscColumnIndexAscSeatNumberAsc(tripId);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
        Map<Long, SeatHold> allTripHolds = seatHoldRepository.findByBusTripId(tripId).stream()
            .filter(h -> h.getSeat() != null && h.getSeat().getId() != null)
            .collect(Collectors.toMap(h -> h.getSeat().getId(), h -> h, (a, b) -> a));

        SeatLayoutResponse response = new SeatLayoutResponse();
        response.setTripId(trip.getId());
        response.setRouteName(trip.getRouteName());
        response.setTotalSeats(trip.getTotalSeats());
        response.setFloorCount(trip.getFloorCount());

        Map<Integer, List<SeatStatusDto>> floors = new LinkedHashMap<>();
        List<SeatStatusDto> selected = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        long available = 0;
        long holdCount = 0;
        long booked = 0;
        LocalDateTime nearestExpiry = null;

        for (Seat seat : seats) {
            SeatHold effectiveHold = allTripHolds.get(seat.getId());
            Seat.SeatStatus effectiveStatus = seat.getSeatStatus();
            LocalDateTime expiresAt = null;
            boolean heldByCurrent = false;

            if (effectiveStatus == Seat.SeatStatus.HOLD) {
                if (effectiveHold == null || effectiveHold.isExpired(now)) {
                    effectiveStatus = Seat.SeatStatus.AVAILABLE;
                } else {
                    expiresAt = effectiveHold.getHoldExpiresAt();
                    heldByCurrent = holderKey != null && holderKey.equals(effectiveHold.getHolderKey());
                    if (!heldByCurrent) {
                        // other user's active hold: show as HOLD in UI (red/orange treatment can vary; spec uses colors)
                        effectiveStatus = Seat.SeatStatus.HOLD;
                    }
                }
            }

            if (effectiveStatus == Seat.SeatStatus.AVAILABLE) available++;
            else if (effectiveStatus == Seat.SeatStatus.BOOKED) booked++;
            else holdCount++;

            SeatStatusDto dto = SeatStatusDto.fromEntity(copySeatWithStatus(seat, effectiveStatus), heldByCurrent, expiresAt);
            floors.computeIfAbsent(seat.getFloorNumber(), k -> new ArrayList<>()).add(dto);

            if (heldByCurrent && effectiveStatus == Seat.SeatStatus.HOLD) {
                selected.add(dto);
                if (seat.getPrice() != null) {
                    total = total.add(seat.getPrice());
                }
                if (expiresAt != null && (nearestExpiry == null || expiresAt.isBefore(nearestExpiry))) {
                    nearestExpiry = expiresAt;
                }
            }
        }

        response.setFloors(floors);
        response.setSelectedSeats(selected);
        response.setTotalSelectedPrice(total);
        response.setAvailableCount(available);
        response.setHoldCount(holdCount);
        response.setBookedCount(booked);
        if (nearestExpiry != null) {
            long seconds = Math.max(0, Duration.between(now, nearestExpiry).getSeconds());
            response.setHoldSecondsRemaining(seconds);
        }
        return response;
    }

    @Override
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public int releaseExpiredHolds() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
        List<Long> expired = seatHoldRepository.findExpiredSeatIds(now);
        int released = 0;
        for (Long seatId : expired) {
            Seat seat = seatRepository.findSeatForUpdate(seatId).orElse(null);
            if (seat == null) continue;
            SeatHold current = seatHoldRepository.findBySeatId(seat.getId()).orElse(null);
            if (current != null && current.isExpired(now)) {
                seatHoldRepository.delete(current);
                if (seat.getSeatStatus() == Seat.SeatStatus.HOLD) {
                    seat.setSeatStatus(Seat.SeatStatus.AVAILABLE);
                }
                released++;
            }
        }
        return released;
    }

    /**
     * Periodic cleanup as a safety net. On-demand checks in hold/release/layout
     * already handle expiration, but this keeps the database tidy.
     */
    @Scheduled(fixedDelayString = "${seat.hold.cleanup-delay-ms:60000}")
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void scheduledReleaseExpiredHolds() {
        try {
            releaseExpiredHolds();
        } catch (Exception ex) {
            log.error("Failed to release expired seat holds", ex);
        }
    }

    private void validateHolderKey(String holderKey) {
        if (holderKey == null || holderKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Holder key is required to manage seat holds");
        }
    }

    private String maskHolder(String holderKey) {
        if (holderKey == null || holderKey.length() <= 6) return "***";
        return holderKey.substring(0, 4) + "***" + holderKey.substring(holderKey.length() - 2);
    }

    private Seat copySeatWithStatus(Seat seat, Seat.SeatStatus status) {
        Seat copy = new Seat();
        copy.setId(seat.getId());
        copy.setBusTrip(seat.getBusTrip());
        copy.setSeatNumber(seat.getSeatNumber());
        copy.setFloorNumber(seat.getFloorNumber());
        copy.setRowIndex(seat.getRowIndex());
        copy.setColumnIndex(seat.getColumnIndex());
        copy.setPrice(seat.getPrice());
        copy.setSeatType(seat.getSeatType());
        copy.setBenefits(seat.getBenefits());
        copy.setSeatStatus(status);
        return copy;
    }
}
