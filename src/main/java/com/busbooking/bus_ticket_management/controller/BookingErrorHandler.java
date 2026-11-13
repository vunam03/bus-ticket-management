package com.busbooking.bus_ticket_management.controller;

import com.busbooking.bus_ticket_management.exception.BookingException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@ControllerAdvice(assignableTypes = {TicketController.class, PaymentController.class})
public class BookingErrorHandler {
    @ExceptionHandler(BookingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String business(BookingException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "booking/error";
    }

    @ExceptionHandler({
        DataIntegrityViolationException.class,
        PessimisticLockingFailureException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    public String conflict(Model model) {
        model.addAttribute(
                "message",
                "Dữ liệu vừa thay đổi. Hãy xem lại vé hoặc chọn ghế lại trước khi thử tiếp.");
        return "booking/error";
    }
}
