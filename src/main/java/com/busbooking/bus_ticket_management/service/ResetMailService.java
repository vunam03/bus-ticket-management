package com.busbooking.bus_ticket_management.service;


/** Account email transport shared by reset and confirmation flows. */
public interface ResetMailService {
    void sendPasswordReset(String email, String resetUrl);
    void sendEmailVerification(String email, String verificationUrl, boolean emailChange);
}
