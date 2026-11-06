package com.busbooking.bus_ticket_management.dto;

import jakarta.validation.constraints.*;
import com.busbooking.bus_ticket_management.validator.StrongPassword;

/**
 * One-time reset token and a new strong password.
 */
public class ResetPasswordRequest {

    @NotBlank(message = "Thiếu mã đặt lại mật khẩu.")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Link đặt lại mật khẩu không hợp lệ.")
    private String token;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
    private String confirmPassword;

    public ResetPasswordRequest() {
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }


}
