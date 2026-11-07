package com.busbooking.bus_ticket_management.dto;

import jakarta.validation.constraints.*;
import com.busbooking.bus_ticket_management.validator.StrongPassword;

/**
 * Editable contact details. Username and role cannot be edited through this DTO.
 */
public class UpdateProfileRequest {

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

    @NotBlank(message = "Nhập mật khẩu hiện tại để xác nhận cập nhật.")
    @Size(max = 64)
    private String currentPassword;

    public UpdateProfileRequest() {
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

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }


}
