package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.model.PaymentMethodType;

import org.springframework.stereotype.Component;

@Component
public class EWalletPayment extends AbstractDemoPayment {
    @Override
    public PaymentMethodType type() {
        return PaymentMethodType.E_WALLET;
    }

    @Override
    protected String successMessage() {
        return "Thanh toán qua ví thử nghiệm thành công.";
    }
}
