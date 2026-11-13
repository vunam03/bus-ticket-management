package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.TicketDetailDto;
import com.busbooking.bus_ticket_management.exception.BookingException;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
public class TicketService {
    private final TicketRepository tickets;
    private final SeatRepository seats;
    private final PaymentTransactionRepository payments;
    private final CancellationPolicy policy;
    private final Clock clock;

    public TicketService(
            TicketRepository tickets,
            SeatRepository seats,
            PaymentTransactionRepository payments,
            CancellationPolicy policy,
            Clock clock) {
        this.tickets = tickets;
        this.seats = seats;
        this.payments = payments;
        this.policy = policy;
        this.clock = clock;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"));
    }

    public Ticket ownedLocked(Long userId, Long id) {
        Ticket ticket =
                tickets.findByIdForUpdate(id)
                        .orElseThrow(() -> new BookingException("Không tìm thấy vé."));
        if (!ticket.getUser().getId().equals(userId))
            throw new BookingException("Không tìm thấy vé.");
        return ticket;
    }

    @Transactional(readOnly = true)
    public TicketDetailDto detail(Long userId, String code) {
        return view(
                tickets.findByTicketCodeAndUser_Id(code, userId)
                        .orElseThrow(() -> new BookingException("Không tìm thấy vé của bạn.")));
    }

    @Transactional(readOnly = true)
    public Page<TicketDetailDto> list(Long userId, TicketStatus status, int page) {
        Pageable pageable = PageRequest.of(Math.max(0, page), 8);
        Page<Ticket> result =
                status == null
                        ? tickets.findByUser_IdOrderByCreatedAtDescIdDesc(userId, pageable)
                        : tickets.findByUser_IdAndTicketStatusOrderByCreatedAtDescIdDesc(
                                userId, status, pageable);
        return result.map(this::view);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransaction> history(Long userId, String code) {
        Ticket t =
                tickets.findByTicketCodeAndUser_Id(code, userId)
                        .orElseThrow(() -> new BookingException("Không tìm thấy vé của bạn."));
        return payments.findByTicket_IdOrderByCreatedAtDesc(t.getId());
    }

    public TicketDetailDto view(Ticket t) {
        LocalDateTime departure = t.getBusTrip().getDepartureTime();
        boolean active = t.getTicketStatus() != TicketStatus.CANCELLED;
        boolean unpaid = t.getPaymentStatus() == PaymentStatus.UNPAID;
        boolean cancellable = active && (unpaid || policy.canCancel(departure, now()));
        BigDecimal refund =
                t.getPaymentStatus() == PaymentStatus.PAID
                        ? policy.refund(t.getTotalAmount(), departure, now())
                        : BigDecimal.ZERO;
        boolean payable =
                active
                        && unpaid
                        && departure.isAfter(now())
                        && (t.getExpiresAt() == null || t.getExpiresAt().isAfter(clock.instant()));
        return new TicketDetailDto(
                t.getId(),
                t.getTicketCode(),
                t.getBusTrip().getRouteName(),
                t.getBusTrip().getBusCompany().getName(),
                t.getUser().getFullName(),
                t.getSeat().getSeatNumber(),
                t.getSeatType(),
                t.getBenefits(),
                departure,
                t.getTotalAmount(),
                t.getTicketStatus(),
                t.getPaymentStatus(),
                t.getPaymentMethod(),
                t.getCreatedAt(),
                t.getExpiresAt(),
                t.getCancelledAt(),
                t.getRefundAmount(),
                refund,
                cancellable,
                payable,
                t.getCancellationReason());
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public String cancel(Long userId, Long id, String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 255) {
            throw new BookingException("Lý do hủy cần từ 1 đến 255 ký tự.");
        }
        Ticket t = ownedLocked(userId, id);
        if (t.getTicketStatus() == TicketStatus.CANCELLED) return t.getTicketCode();
        if (t.getPaymentStatus() == PaymentStatus.PAID) {
            if (!policy.canCancel(t.getBusTrip().getDepartureTime(), now())) {
                throw new BookingException(
                        "Vé đã thanh toán chỉ được hủy trước giờ khởi hành ít nhất 6 giờ.");
            }
            BigDecimal refund =
                    policy.refund(t.getTotalAmount(), t.getBusTrip().getDepartureTime(), now());
            PaymentTransaction transaction = PaymentTransaction.refund(t, refund);
            transaction.markSuccess("DEMO-REFUND-" + java.util.UUID.randomUUID());
            payments.save(transaction);
            t.setRefundAmount(refund);
            t.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        release(t, reason.trim());
        return t.getTicketCode();
    }

    void release(Ticket t, String reason) {
        Seat seat =
                seats.findByIdAndTripIdForUpdate(t.getSeat().getId(), t.getBusTrip().getId())
                        .orElseThrow(() -> new BookingException("Ghế của vé không còn tồn tại."));
        t.setTicketStatus(TicketStatus.CANCELLED);
        t.setCancelledAt(clock.instant());
        t.setCancellationReason(reason);
        seat.setSeatStatus(Seat.SeatStatus.AVAILABLE);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void expire(Long id) {
        Ticket t = tickets.findByIdForUpdate(id).orElse(null);
        if (t != null
                && t.getTicketStatus() == TicketStatus.PENDING
                && t.getPaymentStatus() == PaymentStatus.UNPAID
                && t.getExpiresAt() != null
                && !t.getExpiresAt().isAfter(clock.instant())) {
            release(t, "Hết thời gian thanh toán 10 phút.");
        }
    }
}
