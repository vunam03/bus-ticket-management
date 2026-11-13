package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.PaymentGatewayDto;

import java.math.BigDecimal;
import java.util.UUID;

/** Common simulation behavior; subclasses describe each payment channel. */
public abstract class AbstractDemoPayment implements PaymentMethod {
    protected abstract String successMessage();

    @Override
    public PaymentGatewayDto simulate(BigDecimal amount, boolean success) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền phải lớn hơn 0.");
        }
        return success
                ? new PaymentGatewayDto(
                        true, "DEMO-" + type() + "-" + UUID.randomUUID(), successMessage())
                : new PaymentGatewayDto(
                        false, null, "Giao dịch thử nghiệm chưa thành công. Bạn có thể thử lại.");
    }
}
