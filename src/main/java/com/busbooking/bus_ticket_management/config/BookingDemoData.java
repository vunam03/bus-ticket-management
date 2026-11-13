package com.busbooking.bus_ticket_management.config;

import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;
import com.busbooking.bus_ticket_management.service.SeatService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Component
@Profile("booking-demo & local")
public class BookingDemoData implements CommandLineRunner {
    @org.springframework.beans.factory.annotation.Autowired
    private SeatRepository seats;
    private final Clock clock;
    private final UserRepository users;
    private final RoleRepository roles;
    private final BusCompanyRepository companies;
    private final BusTripRepository trips;
    private final SeatService seatService;
    private final PasswordEncoder encoder;

    public BookingDemoData(
            UserRepository users,
            RoleRepository roles,
            BusCompanyRepository companies,
            BusTripRepository trips,
            SeatService seatService,
            PasswordEncoder encoder,
            Clock clock) {
        this.clock = clock;
        this.users = users;
        this.roles = roles;
        this.companies = companies;
        this.trips = trips;
        this.seatService = seatService;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!users.existsByUsername("demo_dien")) {
            Role role =
                    roles.findByName(Role.CUSTOMER)
                            .orElseGet(() -> roles.save(new Role(Role.CUSTOMER)));
            User user = new User();
            user.setUsername("demo_dien");
            user.setEmail("demo_dien@example.invalid");
            user.setFullName("Tôn Gia Diễn");
            user.setPhone("0900000000");
            user.setPassword(encoder.encode("DemoTicket2026!"));
            user.setRole(role);
            user.setEnabled(true);
            user.setEmailVerified(true);
            users.save(user);
        }
        boolean exists =
                trips.findAll().stream()
                        .anyMatch(
                                t ->
                                        ("[DEMO] Hà Nội - Hải Phòng".equals(t.getRouteName()) || "Hà Nội - Hải Phòng".equals(t.getRouteName()))
                                                && t.getDepartureTime()
                                                        .isAfter(
                                                                LocalDateTime.ofInstant(
                                                                        clock.instant(),
                                                                        ZoneId.of(
                                                                                "Asia/Ho_Chi_Minh"))));
        if (exists) return;
        BusCompany company =
                companies.save(
                        new BusCompany(null, "Hành Trình", "0900000000", "Hà Nội", 4.8));
        BusTrip trip = new BusTrip();
        trip.setRouteName("Hà Nội - Hải Phòng");
        trip.setDepartureLocation("Hà Nội");
        trip.setArrivalLocation("Hải Phòng");
        trip.setDepartureTime(
                LocalDateTime.ofInstant(clock.instant(), ZoneId.of("Asia/Ho_Chi_Minh"))
                        .plusDays(3)
                        .withHour(8)
                        .withMinute(30)
                        .withSecond(0)
                        .withNano(0));
        trip.setTicketPrice(new BigDecimal("150000"));
        trip.setBasePrice(new BigDecimal("150000"));
        trip.setTotalSeats(40);
        trip.setFloorCount(2);
        trip.setBusType("Giường nằm 40 chỗ");
        trip.setBusCompany(company);
        trips.saveAndFlush(trip);
        seatService.initializeSeatsForTrip(trip);
        seats.findByBusTripIdOrderByFloorNumberAscRowIndexAscColumnIndexAscSeatNumberAsc(trip.getId())
                .forEach(s -> {
                    boolean vip = s.getFloorNumber() == 1 && s.getRowIndex() <= 2;
                    s.setBenefits(vip
                        ? "Giường rộng; nước uống; chăn riêng; bộ bịt mắt và nút tai."
                        : "Giường tiêu chuẩn; điều hòa; nước uống.");
                    if (vip) { s.setSeatType(SeatType.VIP); s.setPrice(new BigDecimal("220000")); }
                });
    }
}
