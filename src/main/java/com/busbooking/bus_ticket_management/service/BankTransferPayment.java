package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.model.PaymentMethodType;

import org.springframework.stereotype.Component;

@Component
public class BankTransferPayment extends AbstractDemoPayment {
    @Override
    public PaymentMethodType type() {
        return PaymentMethodType.BANK_TRANSFER;
    }

    @Override
    protected String successMessage() {
        return "Thanh toán chuyển khoản thử nghiệm thành công.";
    }
}
