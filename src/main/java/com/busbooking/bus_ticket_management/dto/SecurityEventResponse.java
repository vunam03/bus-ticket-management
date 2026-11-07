package com.busbooking.bus_ticket_management.dto;


/** Safe display projection; excludes account identifiers and authentication secrets. */
public record SecurityEventResponse(Long id, String action, String occurredAt,
                                    String ipAddress, String device) {
}
