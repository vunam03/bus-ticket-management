package com.busbooking.bus_ticket_management.controller;

import com.busbooking.bus_ticket_management.dto.SeatLayoutResponse;
import com.busbooking.bus_ticket_management.dto.SeatSelectRequest;
import com.busbooking.bus_ticket_management.dto.SeatStatusDto;
import com.busbooking.bus_ticket_management.exception.SeatAlreadyBookedException;
import com.busbooking.bus_ticket_management.service.SeatLockService;
import com.busbooking.bus_ticket_management.service.SeatService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Provides the seat selection page and REST endpoints for real-time seat holding.
 * AJAX polling is used by the UI to refresh seat statuses without a full page reload.
 */
@Controller
public class SeatController {

    private static final String HOLDER_KEY_ATTR = "seatHolderKey";

    private final SeatLockService seatLockService;
    private final SeatService seatService;

    public SeatController(SeatLockService seatLockService, SeatService seatService) {
        this.seatLockService = seatLockService;
        this.seatService = seatService;
    }

    /**
     * Renders the interactive 2-floor seat selection page for a trip.
     */
    @GetMapping("/booking/trips/{tripId}/seats")
    public String seatSelectionPage(@PathVariable Long tripId, HttpSession session, Model model) {
        seatService.ensureSeatsInitialized(tripId);
        String holderKey = resolveHolderKey(session);
        SeatLayoutResponse layout = seatLockService.getSeatLayout(tripId, holderKey);
        model.addAttribute("tripId", tripId);
        model.addAttribute("layout", layout);
        model.addAttribute("holderKey", holderKey);
        return "booking/seat-selection";
    }

    /**
     * Returns the current seat layout and statuses as JSON for real-time polling.
     */
    @GetMapping("/booking/api/trips/{tripId}/layout")
    @ResponseBody
    public ResponseEntity<?> getSeatLayout(@PathVariable Long tripId, HttpSession session) {
        try {
            String holderKey = resolveHolderKey(session);
            SeatLayoutResponse layout = seatLockService.getSeatLayout(tripId, holderKey);
            return ResponseEntity.ok(layout);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorPayload("NOT_FOUND", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorPayload("INTERNAL_ERROR", "Không thể tải sơ đồ ghế. Vui lòng thử lại."));
        }
    }

    /**
     * Holds or releases a seat using only the server-owned session holder key.
     */
    @PostMapping("/booking/api/trips/{tripId}/select")
    @ResponseBody
    public ResponseEntity<?> selectSeat(
            @PathVariable Long tripId,
            @Valid @RequestBody SeatSelectRequest request,
            BindingResult bindingResult,
            HttpSession session) {

        if (bindingResult.hasErrors()) {
            String message = bindingResult.getAllErrors().isEmpty()
                ? "Dữ liệu không hợp lệ"
                : bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ResponseEntity.badRequest().body(errorPayload("VALIDATION_ERROR", message));
        }
        if (request.getTripId() != null && !tripId.equals(request.getTripId())) {
            return ResponseEntity.badRequest().body(errorPayload("VALIDATION_ERROR", "Trip id không khớp."));
        }

        String holderKey = resolveHolderKey(session);
        String action = request.getAction() == null ? "" : request.getAction().trim().toUpperCase();
        try {
            SeatStatusDto seatStatus;
            if ("HOLD".equals(action)) {
                seatStatus = seatLockService.holdSeat(tripId, request.getSeatId(), holderKey);
            } else if ("RELEASE".equals(action)) {
                seatStatus = seatLockService.releaseSeat(tripId, request.getSeatId(), holderKey);
            } else {
                return ResponseEntity.badRequest().body(errorPayload("VALIDATION_ERROR", "Action phải là HOLD hoặc RELEASE."));
            }

            SeatLayoutResponse layout = seatLockService.getSeatLayout(tripId, holderKey);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("success", true);
            payload.put("seat", seatStatus);
            payload.put("layout", layout);
            payload.put("timestamp", LocalDateTime.now().toString());
            return ResponseEntity.ok(payload);
        } catch (SeatAlreadyBookedException ex) {
            SeatLayoutResponse layout = safeLayout(tripId, holderKey);
            Map<String, Object> payload = errorPayload("SEAT_CONFLICT", ex.getMessage());
            payload.put("seatId", ex.getSeatId());
            payload.put("seatNumber", ex.getSeatNumber());
            payload.put("layout", layout);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(payload);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorPayload("BAD_REQUEST", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorPayload("INTERNAL_ERROR", "Không thể cập nhật trạng thái ghế."));
        }
    }

    private String resolveHolderKey(HttpSession session) {
        Object existing = session.getAttribute(HOLDER_KEY_ATTR);
        if (existing instanceof String && !((String) existing).trim().isEmpty()) {
            return (String) existing;
        }
        String generated = java.util.UUID.randomUUID().toString();
        session.setAttribute(HOLDER_KEY_ATTR, generated);
        return generated;
    }

    private Map<String, Object> errorPayload(String code, String message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", false);
        payload.put("code", code);
        payload.put("message", message);
        return payload;
    }

    private SeatLayoutResponse safeLayout(Long tripId, String holderKey) {
        try {
            return seatLockService.getSeatLayout(tripId, holderKey);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Fallback for unexpected errors on JSON endpoints in this controller.
     */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorPayload("INTERNAL_ERROR", "Đã xảy ra lỗi hệ thống."));
    }
}
