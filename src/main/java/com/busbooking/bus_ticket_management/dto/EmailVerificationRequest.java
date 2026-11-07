package com.busbooking.bus_ticket_management.dto;


import jakarta.validation.constraints.*;

public class EmailVerificationRequest {
    @NotBlank(message = "Vui lòng nhập email đã đăng ký.")
    @Email(message = "Email không hợp lệ.")
    @Size(max = 254, message = "Email tối đa 254 ký tự.")
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
