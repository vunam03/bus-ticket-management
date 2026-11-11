package com.busbooking.bus_ticket_management.dto;

public record PaymentGatewayDto(

        boolean success,

        String reference,

        String message

) {
}