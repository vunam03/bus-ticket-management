package com.busbooking.bus_ticket_management.config;

import com.busbooking.bus_ticket_management.model.TicketStatus;
import com.busbooking.bus_ticket_management.repository.TicketRepository;
import com.busbooking.bus_ticket_management.service.TicketService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

@Component
public class BookingExpiryTask {
    private static final Logger log = LoggerFactory.getLogger(BookingExpiryTask.class);
    private final TicketRepository tickets;
    private final TicketService service;
    private final Clock clock;

    public BookingExpiryTask(TicketRepository tickets, TicketService service, Clock clock) {
        this.tickets = tickets;
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${booking.expiry-delay-ms:30000}")
    public void expire() {
        for (var t :
                tickets.findByTicketStatusAndExpiresAtBefore(
                        TicketStatus.PENDING, clock.instant())) {
            try {
                service.expire(t.getId());
            } catch (RuntimeException ex) {
                log.warn("Could not expire ticket {}", t.getId(), ex);
            }
        }
    }
}
