package com.busbooking.bus_ticket_management.dto;

import jakarta.validation.constraints.*;
import com.busbooking.bus_ticket_management.validator.StrongPassword;

/**
 * A password change requires proof of the current password.
 */
public class ChangePasswordRequest {

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại.")
    @Size(max = 64)
    private String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @StrongPassword
    private String newPassword;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
    private String confirmPassword;

    public ChangePasswordRequest() {
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
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
