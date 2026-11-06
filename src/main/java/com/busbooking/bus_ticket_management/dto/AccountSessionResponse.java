package com.busbooking.bus_ticket_management.dto;


/** id is an independent management UUID, never a browser session identifier. */
public record AccountSessionResponse(String id, String createdAt, String lastSeenAt,
                                     String expiresAt, String ipAddress, String device,
                                     boolean current) {
}
