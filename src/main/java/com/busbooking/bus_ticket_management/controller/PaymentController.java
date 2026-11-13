package com.busbooking.bus_ticket_management.controller;

import com.busbooking.bus_ticket_management.exception.BookingException;
import com.busbooking.bus_ticket_management.security.AccountPrincipal;
import com.busbooking.bus_ticket_management.service.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PaymentController {
    private final PaymentService payments;
    private final TicketService tickets;

    public PaymentController(PaymentService payments, TicketService tickets) {
        this.payments = payments;
        this.tickets = tickets;
    }

    @PostMapping("/payments/{code}/simulate")
    public String simulate(
            @AuthenticationPrincipal AccountPrincipal user,
            @PathVariable String code,
            @RequestParam boolean success,
            @RequestParam(required = false) String channel,
            RedirectAttributes redirect) {
        if (user == null) throw new BookingException("Vui lòng đăng nhập.");
        var ticket = tickets.detail(user.getId(), code);
        var result = payments.pay(user.getId(), ticket.id(), success, channel);
        redirect.addFlashAttribute(
                result.success() ? "successMessage" : "errorMessage", result.message());
        return "redirect:/user/tickets/" + code;
    }
}
