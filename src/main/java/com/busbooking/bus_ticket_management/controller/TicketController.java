package com.busbooking.bus_ticket_management.controller;

import com.busbooking.bus_ticket_management.dto.*;
import com.busbooking.bus_ticket_management.exception.BookingException;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.security.AccountPrincipal;
import com.busbooking.bus_ticket_management.service.*;
import com.busbooking.bus_ticket_management.utils.*;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
public class TicketController {
    private final BookingService booking;
    private final TicketService tickets;
    private final BusTripService trips;
    private final QRCodeGenerator qr;
    private final PDFExporter pdf;
    private final String baseUrl;
    private final boolean demoEnabled;

    public TicketController(
            BookingService booking,
            TicketService tickets,
            BusTripService trips,
            QRCodeGenerator qr,
            PDFExporter pdf,
            @Value("${app.base-url:http://localhost:8080}") String baseUrl,
            @Value("${booking.demo-payments-enabled:false}") boolean demoEnabled) {
        this.booking = booking;
        this.tickets = tickets;
        this.trips = trips;
        this.qr = qr;
        this.pdf = pdf;
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.demoEnabled = demoEnabled;
    }

    private Long userId(AccountPrincipal principal) {
        if (principal == null) throw new BookingException("Vui lòng đăng nhập để tiếp tục.");
        return principal.getId();
    }

    private String holder(HttpSession session) {
        Object value = session.getAttribute("seatHolderKey");
        return value instanceof String s ? s : null;
    }

    @ModelAttribute("demoPayments")
    public boolean demoPayments() {
        return demoEnabled;
    }

    @GetMapping("/booking/trips")
    public String trips(Model model) {
        model.addAttribute(
                "trips",
                trips.getAllTrips().stream()
                        .filter(t -> t.getDepartureTime().isAfter(booking.now()))
                        .toList());
        return "booking/trips";
    }

    @GetMapping("/booking/checkout")
    public String checkout(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam Long tripId,
            @RequestParam List<Long> seatIds,
            HttpSession session,
            Model model) {
        model.addAttribute(
                "checkout", booking.preview(userId(principal), tripId, seatIds, holder(session)));
        model.addAttribute("bookingKey", UUID.randomUUID().toString());
        model.addAttribute(
                "selectedSeatIds",
                seatIds.stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(",")));
        return "booking/checkout";
    }

    @GetMapping(value = "/booking/checkout/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> checkoutQr(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam Long tripId,
            @RequestParam List<Long> seatIds,
            @RequestParam(defaultValue = "BANK_TRANSFER") PaymentMethodType paymentMethod,
            @RequestParam(required = false) String channel,
            HttpSession session) {
        CheckoutResponse checkout = booking.preview(userId(principal), tripId, seatIds, holder(session));
        if (paymentMethod == PaymentMethodType.CASH) {
            throw new BookingException("Tiền mặt không sử dụng QR thanh toán.");
        }
        String selectedChannel = PaymentService.channelFor(paymentMethod, channel);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(qr.png("TEST ONLY | " + selectedChannel + " | TRIP " + tripId + " | "
                        + checkout.total() + " VND | NO REAL TRANSFER"));
    }

    @PostMapping("/booking/confirm")
    public String confirm(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @ModelAttribute BookingRequest request,
            BindingResult errors,
            HttpSession session) {
        if (errors.hasErrors())
            throw new BookingException("Dữ liệu đặt vé không hợp lệ. Hãy chọn ghế lại.");
        List<Ticket> result = booking.book(userId(principal), request, holder(session));
        return "redirect:/booking/success?bookingKey=" + result.get(0).getBookingKey();
    }

    @GetMapping("/booking/success")
    public String success(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam String bookingKey,
            Model model) {
        model.addAttribute("tickets", booking.findBooking(userId(principal), bookingKey));
        return "booking/booking-success";
    }

    @GetMapping("/user/my-tickets")
    public String list(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {
        model.addAttribute("page", tickets.list(userId(principal), status, page));
        model.addAttribute("selectedStatus", status == null ? "" : status.name());
        return "user/my-tickets";
    }

    @GetMapping("/user/tickets/{code}")
    public String detail(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String code,
            Model model) {
        model.addAttribute("ticket", tickets.detail(userId(principal), code));
        model.addAttribute("transactions", tickets.history(userId(principal), code));
        return "user/ticket-detail";
    }

    @PostMapping("/tickets/cancel")
    public String cancel(
            @AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam Long ticketId,
            @RequestParam String reason,
            RedirectAttributes redirect) {
        String code = tickets.cancel(userId(principal), ticketId, reason);
        redirect.addFlashAttribute(
                "successMessage", "Đã hủy vé. Khoản hoàn được ghi nhận trong lịch sử giao dịch thử nghiệm.");
        return "redirect:/user/tickets/" + code;
    }

    @GetMapping(value = "/user/tickets/{code}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> qr(
            @AuthenticationPrincipal AccountPrincipal principal, @PathVariable String code) {
        TicketDetailDto t = tickets.detail(userId(principal), code);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(qr.png(baseUrl + "/user/tickets/" + t.code()));
    }

    @GetMapping(value = "/user/tickets/{code}/payment-qr", produces = MediaType.IMAGE_PNG_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> paymentQr(
            @AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String code,
            @RequestParam(required = false) String channel) {
        TicketDetailDto t = tickets.detail(userId(principal), code);
        if (t.method() == PaymentMethodType.CASH) {
            throw new BookingException("Vé thanh toán tiền mặt không sử dụng QR thanh toán.");
        }
        String selectedChannel = PaymentService.channelFor(t.method(), channel);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(qr.png("TEST ONLY | " + selectedChannel + " | " + t.code() + " | "
                        + t.total() + " VND | NO REAL TRANSFER"));
    }

    @GetMapping(value = "/user/tickets/{code}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @ResponseBody
    public ResponseEntity<byte[]> pdf(
            @AuthenticationPrincipal AccountPrincipal principal, @PathVariable String code) {
        TicketDetailDto t = tickets.detail(userId(principal), code);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ticket-" + t.code() + ".pdf")
                .body(pdf.export(t, baseUrl + "/user/tickets/" + t.code()));
    }
}
