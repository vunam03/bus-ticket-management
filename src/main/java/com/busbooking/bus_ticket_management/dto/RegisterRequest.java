package com.busbooking.bus_ticket_management.dto;

import jakarta.validation.constraints.*;
import com.busbooking.bus_ticket_management.validator.StrongPassword;

/**
 * Public registration fields; role and enabled state are assigned on the server.
 */
public class RegisterRequest {

    @NotBlank(message = "Vui lòng nhập tên đăng nhập.")
    @Pattern(regexp = "[a-zA-Z0-9_]{4,30}", message = "Tên đăng nhập gồm 4–30 chữ cái, số hoặc dấu gạch dưới.")
    private String username;

    @NotBlank(message = "Vui lòng nhập họ tên.")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email.")
    @Email(message = "Email không hợp lệ.")
    @Size(max = 254, message = "Email tối đa 254 ký tự.")
    private String email;

    @Pattern(regexp = "^$|[+]?[0-9]{9,15}", message = "Số điện thoại cần 9–15 chữ số, có thể bắt đầu bằng +.")
    private String phone;

    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự.")
    private String address;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @StrongPassword
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu.")
    private String confirmPassword;

    public RegisterRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }


}
