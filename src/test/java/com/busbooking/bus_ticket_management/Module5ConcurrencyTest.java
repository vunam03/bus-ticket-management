package com.busbooking.bus_ticket_management;

import com.busbooking.bus_ticket_management.dto.BookingRequest;
import com.busbooking.bus_ticket_management.exception.BookingException;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;
import com.busbooking.bus_ticket_management.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"booking.demo-payments-enabled=true", "booking.expiry-delay-ms=3600000"})
@ActiveProfiles("test")
class Module5ConcurrencyTest {
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired BusCompanyRepository companies;
    @Autowired BusTripRepository trips;
    @Autowired SeatRepository seats;
    @Autowired TicketRepository tickets;
    @Autowired PaymentTransactionRepository transactions;
    @Autowired BookingService booking;
    @Autowired SeatLockService locking;
    @Autowired PaymentService payments;
    @Autowired PlatformTransactionManager manager;

    record Fixture(Long userId, Long tripId, Long seatId, String holder) { }

    Fixture fixture() {
        Fixture f = new TransactionTemplate(manager).execute(status -> {
            Role role = roles.findByName(Role.CUSTOMER).orElseGet(() -> roles.save(new Role(Role.CUSTOMER)));
            String key = UUID.randomUUID().toString().replace("-", "").substring(0, 20);
            User user = new User();
            user.setUsername(key); user.setEmail(key + "@example.com");
            user.setPassword("test-hash"); user.setFullName("Concurrent test");
            user.setRole(role); user.setEnabled(true); user.setEmailVerified(true);
            users.saveAndFlush(user);
            BusCompany company = companies.save(new BusCompany(null, "Concurrent company", "0900000000", "HN", 4.0));
            BusTrip trip = new BusTrip();
            trip.setRouteName("Concurrent trip"); trip.setDepartureLocation("HN"); trip.setArrivalLocation("HP");
            trip.setDepartureTime(booking.now().plusDays(3));
            trip.setTicketPrice(new BigDecimal("150000")); trip.setBasePrice(new BigDecimal("150000"));
            trip.setTotalSeats(40); trip.setFloorCount(2); trip.setBusCompany(company);
            trips.saveAndFlush(trip);
            Seat seat = seats.saveAndFlush(new Seat(trip, "A01", 1, 1, 1, new BigDecimal("150000")));
            return new Fixture(user.getId(), trip.getId(), seat.getId(), UUID.randomUUID().toString());
        });
        locking.holdSeat(f.tripId(), f.seatId(), f.holder());
        return f;
    }

    <T> List<T> simultaneous(Callable<T> first, Callable<T> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<T>> futures = new ArrayList<>();
            for (Callable<T> task : List.of(first, second)) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
                    return task.call();
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) results.add(future.get(20, TimeUnit.SECONDS));
            return results;
        } finally { pool.shutdownNow(); }
    }

    @Test
    void sameBookingKeySubmittedConcurrentlyCreatesOneTicket() throws Exception {
        Fixture f = fixture();
        String key = UUID.randomUUID().toString();
        BookingRequest request = new BookingRequest(f.tripId(), List.of(f.seatId()), PaymentMethodType.CASH, key);
        Callable<Long> action = () -> booking.book(f.userId(), request, f.holder()).get(0).getId();
        List<Long> result = simultaneous(action, action);
        assertEquals(result.get(0), result.get(1));
        assertEquals(1, tickets.findByUser_IdAndBookingKeyOrderByIdAsc(f.userId(), key).size());
    }

    @Test
    void differentKeysCannotReserveSameSeatTwice() throws Exception {
        Fixture f = fixture();
        Callable<Boolean> action = () -> {
            BookingRequest request = new BookingRequest(f.tripId(), List.of(f.seatId()), PaymentMethodType.CASH,
                    UUID.randomUUID().toString());
            try { booking.book(f.userId(), request, f.holder()); return true; }
            catch (BookingException expected) { return false; }
        };
        List<Boolean> result = simultaneous(action, action);
        assertEquals(1, result.stream().filter(Boolean::booleanValue).count());
    }

    @Test
    void simultaneousPaymentCreatesOnlyOneSuccessfulTransaction() throws Exception {
        Fixture f = fixture();
        var request = new BookingRequest(f.tripId(), List.of(f.seatId()), PaymentMethodType.E_WALLET,
                UUID.randomUUID().toString());
        Long ticketId = booking.book(f.userId(), request, f.holder()).get(0).getId();
        Callable<Boolean> action = () -> payments.pay(f.userId(), ticketId, true).success();
        assertEquals(List.of(true, true), simultaneous(action, action));
        assertEquals(1, transactions.findByTicket_IdOrderByCreatedAtDesc(ticketId).size());
    }
    @Test
    void invalidSecondSeatRollsBackEntireBooking() {
        Fixture f = fixture();
        String key = UUID.randomUUID().toString();
        var request = new BookingRequest(f.tripId(), List.of(f.seatId(), Long.MAX_VALUE),
                PaymentMethodType.CASH, key);
        assertThrows(BookingException.class, () -> booking.book(f.userId(), request, f.holder()));
        assertTrue(tickets.findByUser_IdAndBookingKeyOrderByIdAsc(f.userId(), key).isEmpty());
        assertEquals(Seat.SeatStatus.HOLD, seats.findById(f.seatId()).orElseThrow().getSeatStatus());
        assertEquals(1, booking.preview(f.userId(), f.tripId(), List.of(f.seatId()), f.holder()).seats().size());
    }

}
