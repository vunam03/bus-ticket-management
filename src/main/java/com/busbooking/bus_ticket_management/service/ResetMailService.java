package com.busbooking.bus_ticket_management.service;


public interface ResetMailService {
    void sendPasswordReset(String email, String resetUrl);
}
