package com.busbooking.bus_ticket_management.dto;


import jakarta.validation.constraints.*;

public class ConfirmEmailRequest {
    @NotBlank(message = "Thiếu mã xác nhận email.")
    @Pattern(regexp = "[A-Za-z0-9_-]{43}", message = "Link xác nhận email không hợp lệ.")
    private String token;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
