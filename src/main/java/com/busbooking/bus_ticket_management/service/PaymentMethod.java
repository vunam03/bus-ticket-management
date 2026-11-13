package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.PaymentGatewayDto;
import com.busbooking.bus_ticket_management.model.PaymentMethodType;

import java.math.BigDecimal;

public interface PaymentMethod {
    PaymentMethodType type();

    PaymentGatewayDto simulate(BigDecimal amount, boolean success);
}
