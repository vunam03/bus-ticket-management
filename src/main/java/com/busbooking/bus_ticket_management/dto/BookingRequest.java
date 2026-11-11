package com.busbooking.bus_ticket_management.dto;

import com.busbooking.bus_ticket_management.model.PaymentMethodType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BookingRequest(

        @NotNull(message = "Vui lòng chọn chuyến xe")
        @Positive(message = "Mã chuyến xe không hợp lệ")
        Long tripId,

        @NotEmpty(message = "Vui lòng chọn ít nhất một ghế")
        @Size(max = 6, message = "Mỗi lần chỉ được đặt tối đa 6 ghế")
        List<@NotNull @Positive Long> seatIds,

        @NotNull(message = "Vui lòng chọn phương thức thanh toán")
        PaymentMethodType paymentMethod,

        @NotBlank(message = "Thiếu mã xác nhận đặt vé")
        @Pattern(
                regexp = "[a-fA-F0-9-]{36}",
                message = "Mã xác nhận đặt vé không hợp lệ"
        )
        String bookingKey

) {
}