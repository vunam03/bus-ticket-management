package com.busbooking.bus_ticket_management.service.impl;

import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.model.Seat;
import com.busbooking.bus_ticket_management.repository.BusTripRepository;
import com.busbooking.bus_ticket_management.repository.SeatRepository;
import com.busbooking.bus_ticket_management.service.SeatService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates dynamic 2-floor seat layouts for bus trips.
 * The generator creates stable seat numbers and grid coordinates so the UI
 * can render the same physical layout consistently across requests.
 */
@Service
public class SeatServiceImpl implements SeatService {

    private static final Logger log = LoggerFactory.getLogger(SeatServiceImpl.class);
    private static final List<Integer> SUPPORTED = Arrays.asList(36, 40, 45);

    private final SeatRepository seatRepository;
    private final BusTripRepository busTripRepository;

    public SeatServiceImpl(SeatRepository seatRepository, BusTripRepository busTripRepository) {
        this.seatRepository = seatRepository;
        this.busTripRepository = busTripRepository;
    }

    @Override
    @Transactional
    public int initializeSeatsForTrip(BusTrip trip) {
        if (trip == null || trip.getId() == null) {
            throw new IllegalArgumentException("Trip must be persisted before initializing seats");
        }
        long existing = seatRepository.countByBusTripId(trip.getId());
        if (existing > 0) {
            log.debug("Seats already initialized for trip {}", trip.getId());
            return 0;
        }

        int totalSeats = trip.getTotalSeats() != null ? trip.getTotalSeats() : 40;
        int floorCount = trip.getFloorCount() != null && trip.getFloorCount() > 0 ? trip.getFloorCount() : 2;
        BigDecimal basePrice = trip.getBasePrice() != null ? trip.getBasePrice() : new BigDecimal("150000");

        List<Seat> seats = buildLayout(trip, totalSeats, floorCount, basePrice);
        seatRepository.saveAll(seats);
        log.info("Initialized {} seats for trip {} (floors={})", seats.size(), trip.getId(), floorCount);
        return seats.size();
    }

    @Override
    @Transactional
    public void ensureSeatsInitialized(Long tripId) {
        BusTrip trip = busTripRepository.findById(tripId)
            .orElseThrow(() -> new IllegalArgumentException("Trip not found: " + tripId));
        initializeSeatsForTrip(trip);
    }

    @Override
    public List<Integer> getSupportedCapacities() {
        return SUPPORTED;
    }

    /**
     * Builds a 2-floor layout. Seats are split across floors as evenly as possible.
     * Floor 1 uses prefix A, floor 2 uses prefix B. Each floor is arranged in rows
     * with up to 3 columns (left window, aisle gap represented by column index, right window)
     * to resemble a typical sleeper/coach layout and remain easy to render.
     */
    private List<Seat> buildLayout(BusTrip trip, int totalSeats, int floorCount, BigDecimal basePrice) {
        List<Seat> seats = new ArrayList<>(totalSeats);
        if (floorCount < 1) {
            floorCount = 1;
        }

        int basePerFloor = totalSeats / floorCount;
        int remainder = totalSeats % floorCount;

        for (int floor = 1; floor <= floorCount; floor++) {
            int seatsOnFloor = basePerFloor + (floor <= remainder ? 1 : 0);
            char prefix = (char) ('A' + (floor - 1));
            BigDecimal floorPrice = calculateFloorPrice(basePrice, floor, floorCount);
            seats.addAll(buildFloorSeats(trip, floor, prefix, seatsOnFloor, floorPrice));
        }
        return seats;
    }

    private List<Seat> buildFloorSeats(BusTrip trip, int floorNumber, char prefix, int seatCount, BigDecimal floorPrice) {
        List<Seat> seats = new ArrayList<>(seatCount);
        int columns = 3; // visual columns per row (layout grid)
        int row = 1;
        int col = 1;
        for (int i = 1; i <= seatCount; i++) {
            String seatNumber = prefix + String.format("%02d", i);
            seats.add(new Seat(trip, seatNumber, floorNumber, row, col, floorPrice));
            col++;
            if (col > columns) {
                col = 1;
                row++;
            }
        }
        return seats;
    }

    /**
     * Applies a small premium to lower floors when multiple floors exist, while
     * keeping pricing flexible and deterministic.
     */
    private BigDecimal calculateFloorPrice(BigDecimal basePrice, int floor, int floorCount) {
        if (floorCount <= 1) {
            return basePrice.setScale(2, RoundingMode.HALF_UP);
        }
        // Floor 1 is slightly more expensive than upper floors.
        BigDecimal multiplier = (floor == 1) ? new BigDecimal("1.05") : BigDecimal.ONE;
        return basePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }
}
