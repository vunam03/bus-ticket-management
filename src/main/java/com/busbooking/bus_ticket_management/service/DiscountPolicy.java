package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.model.CustomerTier;

import java.math.BigDecimal;

public interface DiscountPolicy {

    BigDecimal discount(BigDecimal subtotal, CustomerTier tier);
}