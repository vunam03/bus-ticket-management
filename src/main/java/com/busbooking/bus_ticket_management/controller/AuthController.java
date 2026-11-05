package com.busbooking.bus_ticket_management.controller;


import com.busbooking.bus_ticket_management.dto.*;
import com.busbooking.bus_ticket_management.exception.*;
import com.busbooking.bus_ticket_management.service.*;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final UserService users;
    private final AuthService auth;

    public AuthController(UserService users, AuthService auth) {
        this.users = users;
        this.auth = auth;
    }

    @GetMapping("/login")
    public String login(@RequestParam(defaultValue = "0") long retrySeconds, Model model) {
        model.addAttribute("retrySeconds", Math.max(0, Math.min(retrySeconds, 900)));
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute RegisterRequest request, BindingResult errors,
                           RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                users.register(request);
                redirect.addFlashAttribute("successMessage", "Đăng ký thành công. Vui lòng mở email xác thực và xác nhận trước khi đăng nhập.");
                return "redirect:/auth/login";
            } catch (UserAlreadyExistsException exception) {
                errors.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            } catch (AccountValidationException exception) {
                errors.rejectValue(exception.getField(), "invalid", exception.getMessage());
            } catch (EmailDeliveryException exception) {
                errors.rejectValue("email", "delivery", exception.getMessage());
            }
        }
        // Password inputs never echo their submitted values.
        request.setPassword(null);
        request.setConfirmPassword(null);
        return "auth/register";
    }

    @GetMapping("/forgot-password")
    public String forgotForm(Model model) {
        model.addAttribute("forgotPasswordRequest", new ForgotPasswordRequest());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgot(@Valid @ModelAttribute ForgotPasswordRequest request, BindingResult errors,
                         RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return "auth/forgot-password";
        }
        try {
            auth.requestPasswordReset(request.getEmail());
        } catch (RuntimeException exception) {
            // Do not expose account existence, SMTP internals or a reset link in the response/log.
            log.error("Password reset delivery failed ({})", exception.getClass().getSimpleName());
        }
        redirect.addFlashAttribute("successMessage", AuthService.RESET_RESPONSE);
        return "redirect:/auth/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(defaultValue = "") String token, Model model) {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(token);
        model.addAttribute("resetPasswordRequest", request);
        model.addAttribute("tokenValid", auth.isResetTokenValid(token));
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String reset(@Valid @ModelAttribute ResetPasswordRequest request, BindingResult errors,
                        Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                auth.resetPassword(request);
                redirect.addFlashAttribute("successMessage", "Đã đặt lại mật khẩu. Vui lòng đăng nhập bằng mật khẩu mới.");
                return "redirect:/auth/login";
            } catch (InvalidResetTokenException exception) {
                errors.rejectValue("token", "invalid", exception.getMessage());
            } catch (AccountValidationException exception) {
                errors.rejectValue(exception.getField(), "invalid", exception.getMessage());
            }
        }
        request.setNewPassword(null);
        request.setConfirmPassword(null);
        model.addAttribute("tokenValid", auth.isResetTokenValid(request.getToken()));
        return "auth/reset-password";
    }

    @GetMapping("/access-denied")
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String accessDenied() {
        return "auth/access-denied";
    }
}
