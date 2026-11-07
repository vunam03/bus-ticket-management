package com.busbooking.bus_ticket_management.controller;


import com.busbooking.bus_ticket_management.dto.*;
import com.busbooking.bus_ticket_management.exception.*;
import com.busbooking.bus_ticket_management.model.EmailTokenPurpose;
import com.busbooking.bus_ticket_management.security.AccountPrincipal;
import com.busbooking.bus_ticket_management.service.EmailVerificationService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.slf4j.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class EmailVerificationController {
    private static final Logger log = LoggerFactory.getLogger(EmailVerificationController.class);
    private final EmailVerificationService verification;

    public EmailVerificationController(EmailVerificationService verification) {
        this.verification = verification;
    }

    @GetMapping("/auth/resend-verification")
    public String resendForm(Model model) {
        model.addAttribute("emailVerificationRequest", new EmailVerificationRequest());
        return "auth/resend-verification";
    }

    @PostMapping("/auth/resend-verification")
    public String resend(@Valid @ModelAttribute EmailVerificationRequest form, BindingResult errors,
                         RedirectAttributes redirect) {
        if (errors.hasErrors()) return "auth/resend-verification";
        try {
            verification.resendRegistration(form.getEmail());
        } catch (EmailDeliveryException exception) {
            log.error("Email confirmation delivery failed");
        }
        redirect.addFlashAttribute("successMessage", EmailVerificationService.RESEND_RESPONSE);
        return "redirect:/auth/resend-verification";
    }

    @GetMapping("/auth/verify-email")
    public String preview(@RequestParam(defaultValue = "") String token, Model model) {
        ConfirmEmailRequest form = new ConfirmEmailRequest();
        form.setToken(token);
        model.addAttribute("confirmEmailRequest", form);
        model.addAttribute("tokenValid", verification.isValid(token));
        return "auth/verify-email";
    }

    @PostMapping("/auth/verify-email")
    public String confirm(@Valid @ModelAttribute ConfirmEmailRequest form, BindingResult errors,
            Authentication authentication, HttpServletRequest request, HttpServletResponse response, Model model) {
        if (!errors.hasErrors()) {
            try {
                var result = verification.confirm(form.getToken());
                if (authentication != null && authentication.getPrincipal() instanceof AccountPrincipal principal
                        && principal.getId().equals(result.userId())) {
                    new SecurityContextLogoutHandler().logout(request, response, authentication);
                }
                return result.purpose() == EmailTokenPurpose.REGISTRATION
                        ? "redirect:/auth/login?emailVerified" : "redirect:/auth/login?emailChanged";
            } catch (InvalidEmailVerificationException exception) {
                errors.rejectValue("token", "invalid", exception.getMessage());
            }
        }
        model.addAttribute("tokenValid", verification.isValid(form.getToken()));
        return "auth/verify-email";
    }

    @PostMapping("/user/email-change/cancel")
    public String cancel(@AuthenticationPrincipal AccountPrincipal principal, RedirectAttributes redirect) {
        verification.cancelEmailChange(principal.getId());
        redirect.addFlashAttribute("successMessage", "Đã hủy yêu cầu đổi email. Địa chỉ hiện tại được giữ nguyên.");
        return "redirect:/user/profile";
    }
}
