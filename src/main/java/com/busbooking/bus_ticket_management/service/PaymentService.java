package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.PaymentGatewayDto;
import com.busbooking.bus_ticket_management.exception.BookingException;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.PaymentTransactionRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.List;

@Service
public class PaymentService {
    private final TicketService tickets;
    private final PaymentTransactionRepository transactions;
    private final List<PaymentMethod> methods;
    private final Clock clock;
    private final boolean demoEnabled;

    public PaymentService(
            TicketService tickets,
            PaymentTransactionRepository transactions,
            List<PaymentMethod> methods,
            Clock clock,
            @Value("${booking.demo-payments-enabled:false}") boolean demoEnabled) {
        this.tickets = tickets;
        this.transactions = transactions;
        this.methods = methods;
        this.clock = clock;
        this.demoEnabled = demoEnabled;
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PaymentGatewayDto pay(Long userId, Long ticketId, boolean success) {
        return pay(userId, ticketId, success, null);
    }

    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public PaymentGatewayDto pay(Long userId, Long ticketId, boolean success, String channel) {
        if (!demoEnabled) throw new BookingException("Thanh toán mô phỏng đang tắt.");
        Ticket t = tickets.ownedLocked(userId, ticketId);
        if (t.getTicketStatus() == TicketStatus.CANCELLED)
            throw new BookingException("Vé đã bị hủy.");
        // The ticket lock serializes concurrent submits; only one successful charge is recorded.
        if (t.getPaymentStatus() == PaymentStatus.PAID) {
            return new PaymentGatewayDto(
                    true, null, "Vé này đã thanh toán; không thu tiền lần nữa.");
        }
        Instant departure =
                t.getBusTrip().getDepartureTime().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        if (!departure.isAfter(clock.instant())
                || (t.getExpiresAt() != null && !t.getExpiresAt().isAfter(clock.instant()))) {
            throw new BookingException("Đã hết thời gian thanh toán. Hãy chọn chuyến và đặt lại.");
        }
        PaymentMethod method =
                methods.stream()
                        .filter(m -> m.type() == t.getPaymentMethod())
                        .findFirst()
                        .orElseThrow(
                                () -> new BookingException("Phương thức thanh toán chưa hỗ trợ."));
        PaymentTransaction transaction =
                new PaymentTransaction(t, t.getTotalAmount(), t.getPaymentMethod());
        transaction.setPaymentChannel(channelFor(t.getPaymentMethod(), channel));
        PaymentGatewayDto result = method.simulate(t.getTotalAmount(), success);
        if (result.success()) {
            transaction.markSuccess(result.reference());
            t.setPaymentStatus(PaymentStatus.PAID);
            t.setTicketStatus(TicketStatus.CONFIRMED);
        } else {
            transaction.markFailed(result.message());
        }
        transactions.save(transaction);
        return result;
    }
    public static String channelFor(PaymentMethodType method, String requested) {
        List<String> allowed = switch (method) {
            case CASH -> List.of("CASH");
            case BANK_TRANSFER -> List.of("VIETCOMBANK", "BIDV", "MB");
            case E_WALLET -> List.of("MOMO", "ZALOPAY", "SHOPEEPAY");
        };
        if (requested == null || requested.isBlank()) return allowed.get(0);
        String normalized = requested.trim().toUpperCase(java.util.Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BookingException("Kênh thanh toán không phù hợp với phương thức của vé.");
        }
        return normalized;
    }

}
