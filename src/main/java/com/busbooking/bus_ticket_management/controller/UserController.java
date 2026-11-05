package com.busbooking.bus_ticket_management.controller;


import com.busbooking.bus_ticket_management.dto.*;
import com.busbooking.bus_ticket_management.exception.*;
import com.busbooking.bus_ticket_management.service.UserService;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user")
public class UserController {
    private final UserService users;

    public UserController(UserService users) {
        this.users = users;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        UserProfileResponse profile = users.profile(authentication.getName());
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName(profile.getFullName());
        request.setEmail(profile.getEmail());
        request.setPhone(profile.getPhone());
        request.setAddress(profile.getAddress());
        model.addAttribute("profile", profile);
        model.addAttribute("updateProfileRequest", request);
        return "user/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication authentication,
            @Valid @ModelAttribute UpdateProfileRequest request, BindingResult errors,
            Model model, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                users.updateProfile(authentication.getName(), request);
                redirect.addFlashAttribute("successMessage", "Đã lưu thông tin. Nếu đổi email, vui lòng xác nhận thư gửi đến địa chỉ mới; email cũ vẫn được giữ đến khi xác nhận.");
                return "redirect:/user/profile";
            } catch (UserAlreadyExistsException exception) {
                errors.rejectValue(exception.getField(), "duplicate", exception.getMessage());
            } catch (InvalidCredentialsException exception) {
                errors.rejectValue(exception.getField(), "invalid", exception.getMessage());
            } catch (EmailDeliveryException exception) {
                errors.rejectValue("email", "delivery", exception.getMessage());
            } catch (AccountValidationException exception) {
                errors.rejectValue(exception.getField(), "invalid", exception.getMessage());
            }
        }
        request.setCurrentPassword(null);
        model.addAttribute("profile", users.profile(authentication.getName()));
        return "user/profile";
    }

    @GetMapping("/change-password")
    public String changeForm(Model model) {
        model.addAttribute("changePasswordRequest", new ChangePasswordRequest());
        return "user/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(Authentication authentication,
            @Valid @ModelAttribute ChangePasswordRequest form, BindingResult errors,
            HttpServletRequest request, HttpServletResponse response, RedirectAttributes redirect) {
        if (!errors.hasErrors()) {
            try {
                users.changePassword(authentication.getName(), form);
                new SecurityContextLogoutHandler().logout(request, response, authentication);
                // Session was invalidated; use a non-sensitive query flag instead of a flash session.
                return "redirect:/auth/login?passwordChanged";
            } catch (InvalidCredentialsException exception) {
                errors.rejectValue(exception.getField(), "invalid", exception.getMessage());
            } catch (AccountValidationException exception) {
                errors.rejectValue(exception.getField(), "invalid", exception.getMessage());
            }
        }
        form.setCurrentPassword(null);
        form.setNewPassword(null);
        form.setConfirmPassword(null);
        return "user/change-password";
    }
}
