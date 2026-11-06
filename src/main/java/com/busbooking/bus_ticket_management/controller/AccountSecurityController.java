package com.busbooking.bus_ticket_management.controller;


import com.busbooking.bus_ticket_management.model.SecurityEventType;
import com.busbooking.bus_ticket_management.security.AccountPrincipal;
import com.busbooking.bus_ticket_management.service.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/security")
public class AccountSecurityController {
    private final SecurityAuditService audit;
    private final AccountSessionService sessions;

    public AccountSecurityController(SecurityAuditService audit, AccountSessionService sessions) {
        this.audit = audit;
        this.sessions = sessions;
    }

    @GetMapping("/history")
    public String history(@AuthenticationPrincipal AccountPrincipal principal,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(required = false) SecurityEventType type, Model model) {
        model.addAttribute("history", audit.history(principal.getId(), type, page));
        model.addAttribute("eventTypes", SecurityEventType.values());
        model.addAttribute("selectedType", type == null ? "" : type.name());
        return "user/security-history";
    }

    @GetMapping("/sessions")
    public String sessions(@AuthenticationPrincipal AccountPrincipal principal,
                           HttpServletRequest request, Model model) {
        model.addAttribute("sessions", sessions.active(principal, request.getSession(false)));
        return "user/sessions";
    }

    @PostMapping("/sessions/{id}/revoke")
    public String revoke(@AuthenticationPrincipal AccountPrincipal principal, @PathVariable String id,
                         Authentication authentication, HttpServletRequest request, HttpServletResponse response,
                         RedirectAttributes redirect) {
        if (sessions.revoke(principal.getId(), id, request.getSession(false))) {
            return logout(authentication, request, response);
        }
        redirect.addFlashAttribute("successMessage", "Đã thu hồi phiên. Phiên đó sẽ bị chặn ở lần truy cập tiếp theo.");
        return "redirect:/user/security/sessions";
    }

    @PostMapping("/sessions/revoke-others")
    public String revokeOthers(@AuthenticationPrincipal AccountPrincipal principal,
                               HttpServletRequest request, RedirectAttributes redirect) {
        sessions.revokeOthers(principal.getId(), request.getSession(false));
        redirect.addFlashAttribute("successMessage", "Đã đăng xuất các phiên khác.");
        return "redirect:/user/security/sessions";
    }

    @PostMapping("/sessions/revoke-all")
    public String revokeAll(@AuthenticationPrincipal AccountPrincipal principal, Authentication authentication,
                            HttpServletRequest request, HttpServletResponse response) {
        sessions.revokeAll(principal.getId());
        return logout(authentication, request, response);
    }

    private String logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        return "redirect:/auth/login?logout";
    }
}
