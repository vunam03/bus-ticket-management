package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.*;
import com.busbooking.bus_ticket_management.exception.BookingException;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Service
public class BookingService {
    private final UserRepository users;
    private final BusTripRepository trips;
    private final SeatRepository seats;
    private final SeatHoldRepository holds;
    private final TicketRepository tickets;
    private final Clock clock;

    public BookingService(
            UserRepository users,
            BusTripRepository trips,
            SeatRepository seats,
            SeatHoldRepository holds,
            TicketRepository tickets,
            Clock clock) {
        this.users = users;
        this.trips = trips;
        this.seats = seats;
        this.holds = holds;
        this.tickets = tickets;
        this.clock = clock;
    }

    public LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    private User customer(Long id) {
        User user =
                users.findById(id)
                        .orElseThrow(() -> new BookingException("Không tìm thấy tài khoản."));
        if (!user.getEnabled() || !user.getEmailVerified()) {
            throw new BookingException("Tài khoản cần được kích hoạt và xác thực email.");
        }
        return user;
    }

    private BigDecimal seatPrice(Seat seat) {
        BigDecimal price = seat.getPrice();
        if (price == null || price.signum() <= 0) {
            throw new BookingException("Ghế chưa được cấu hình giá hợp lệ.");
        }
        price = price.setScale(2, java.math.RoundingMode.HALF_UP);
        if (price.signum() <= 0 || price.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new BookingException("Giá vé ngoài phạm vi hợp lệ.");
        }
        return price;
    }

    private List<Long> checkedIds(List<Long> ids) {
        if (ids == null
                || ids.isEmpty()
                || ids.size() > 6
                || ids.stream().anyMatch(Objects::isNull)
                || new HashSet<>(ids).size() != ids.size()) {
            throw new BookingException("Mỗi lần đặt cần chọn từ 1 đến 6 ghế khác nhau.");
        }
        return ids.stream().sorted().toList();
    }

    private void validateSeat(Seat seat, String holder) {
        if (seat.getSeatStatus() != Seat.SeatStatus.HOLD) {
            throw new BookingException(
                    "Ghế " + seat.getSeatNumber() + " chưa được giữ hoặc đã được đặt.");
        }
        SeatHold hold =
                holds.findBySeatId(seat.getId())
                        .orElseThrow(
                                () ->
                                        new BookingException(
                                                "Lượt giữ ghế đã hết hạn. Hãy chọn lại."));
        if (holder == null || !holder.equals(hold.getHolderKey()) || hold.isExpired(now())) {
            throw new BookingException("Lượt giữ ghế đã hết hạn hoặc không thuộc phiên của bạn.");
        }
    }

    @Transactional(readOnly = true)
    public CheckoutResponse preview(Long userId, Long tripId, List<Long> ids, String holder) {
        User user = customer(userId);
        BusTrip trip =
                trips.findById(tripId)
                        .orElseThrow(() -> new BookingException("Chuyến xe không tồn tại."));
        if (!trip.getDepartureTime().isAfter(now()))
            throw new BookingException("Chuyến xe đã khởi hành.");
        List<CheckoutResponse.SeatLine> lines = new ArrayList<>();
        LocalDateTime nearest = null;
        BigDecimal total = BigDecimal.ZERO;
        for (Long id : checkedIds(ids)) {
            Seat seat =
                    seats.findByIdAndBusTripId(id, tripId)
                            .orElseThrow(() -> new BookingException("Ghế không thuộc chuyến xe."));
            validateSeat(seat, holder);
            BigDecimal price = seatPrice(seat);
            lines.add(new CheckoutResponse.SeatLine(id, seat.getSeatNumber(), price, seat.getSeatType(), seat.getBenefits()));
            total = total.add(price);
            LocalDateTime expiry = holds.findBySeatId(id).orElseThrow().getHoldExpiresAt();
            if (nearest == null || expiry.isBefore(nearest)) nearest = expiry;
        }
        return new CheckoutResponse(
                tripId,
                trip.getRouteName(),
                trip.getBusCompany().getName(),
                trip.getDepartureTime(),
                lines,
                total,
                nearest);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public List<Ticket> book(Long userId, BookingRequest request, String holder) {
        // Serialize requests from one account: repeated submit returns the original tickets.
        users.findLockedById(userId)
                .orElseThrow(() -> new BookingException("Không tìm thấy tài khoản."));
        User user = customer(userId);
        if (request.bookingKey() == null
                || !request.bookingKey().matches("[a-fA-F0-9-]{36}")
                || request.paymentMethod() == null
                || request.tripId() == null) {
            throw new BookingException("Yêu cầu đặt vé không hợp lệ.");
        }
        List<Long> ids = checkedIds(request.seatIds());
        List<Ticket> existing =
                tickets.findByUser_IdAndBookingKeyOrderByIdAsc(userId, request.bookingKey());
        if (!existing.isEmpty()) {
            List<Long> prior = existing.stream().map(t -> t.getSeat().getId()).sorted().toList();
            if (!prior.equals(ids)
                    || !existing.get(0).getBusTrip().getId().equals(request.tripId())
                    || existing.get(0).getPaymentMethod() != request.paymentMethod()) {
                throw new BookingException("Mã yêu cầu đã được dùng cho lựa chọn khác.");
            }
            return existing;
        }
        BusTrip trip =
                trips.findById(request.tripId())
                        .orElseThrow(() -> new BookingException("Chuyến xe không tồn tại."));
        if (!trip.getDepartureTime().isAfter(now()))
            throw new BookingException("Chuyến xe đã khởi hành.");
        List<Ticket> result = new ArrayList<>();
        // Always lock seats in ascending ID order to avoid multi-seat deadlocks.
        for (Long id : ids) {
            Seat seat =
                    seats.findByIdAndTripIdForUpdate(id, trip.getId())
                            .orElseThrow(() -> new BookingException("Ghế không thuộc chuyến xe."));
            validateSeat(seat, holder);
            if (tickets.existsBySeat_IdAndTicketStatusIn(
                    id, List.of(TicketStatus.PENDING, TicketStatus.CONFIRMED))) {
                throw new BookingException("Ghế đã có vé đang hoạt động.");
            }
            Ticket ticket = new Ticket(user, trip, seat, seatPrice(seat), request.paymentMethod());
            ticket.setSeatType(seat.getSeatType());
            ticket.setBenefits(seat.getBenefits());
            ticket.setBookingKey(request.bookingKey());
            ticket.setRefundAmount(BigDecimal.ZERO);
            Instant departure =
                    trip.getDepartureTime().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
            Instant deadline = clock.instant().plusSeconds(600);
            ticket.setExpiresAt(deadline.isBefore(departure) ? deadline : departure);
            result.add(tickets.save(ticket));
            holds.findBySeatId(id).ifPresent(holds::delete);
            // Reserved for the pending ticket, unavailable to the seat-hold module.
            seat.setSeatStatus(Seat.SeatStatus.BOOKED);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> findBooking(Long userId, String key) {
        List<Ticket> found = tickets.findByUser_IdAndBookingKeyOrderByIdAsc(userId, key);
        if (found.isEmpty()) throw new BookingException("Không tìm thấy đơn đặt vé của bạn.");
        return found.stream()
                .map(
                        t -> {
                            java.util.Map<String, Object> item = new java.util.LinkedHashMap<>();
                            item.put("code", t.getTicketCode());
                            item.put("seat", t.getSeat().getSeatNumber());
                            item.put("route", t.getBusTrip().getRouteName());
                            item.put("amount", t.getTotalAmount());
                            item.put("status", t.getTicketStatus());
                            return item;
                        })
                .toList();
    }
}
