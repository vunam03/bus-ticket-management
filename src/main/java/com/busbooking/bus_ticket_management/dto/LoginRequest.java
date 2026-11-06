package com.busbooking.bus_ticket_management.dto;

import jakarta.validation.constraints.*;
import com.busbooking.bus_ticket_management.validator.StrongPassword;

/**
 * Login form. Spring Security processes POST /auth/login.
 */
public class LoginRequest {

    @NotBlank(message = "Vui lòng nhập tên đăng nhập.")
    @Pattern(regexp = "[a-zA-Z0-9_]{4,30}", message = "Tên đăng nhập gồm 4–30 chữ cái, số hoặc dấu gạch dưới.")
    private String username;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @Size(max = 64)
    private String password;

    public LoginRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


}
