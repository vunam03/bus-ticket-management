package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.model.PaymentMethodType;

import org.springframework.stereotype.Component;

@Component
public class CashPayment extends AbstractDemoPayment {
    @Override
    public PaymentMethodType type() {
        return PaymentMethodType.CASH;
    }

    @Override
    protected String successMessage() {
        return "Đã ghi nhận tiền mặt trong phiên thử nghiệm.";
    }
}
