package com.busbooking.bus_ticket_management;

import com.busbooking.bus_ticket_management.dto.*;
import com.busbooking.bus_ticket_management.exception.BookingException;
import com.busbooking.bus_ticket_management.model.*;
import com.busbooking.bus_ticket_management.repository.*;
import com.busbooking.bus_ticket_management.security.AccountPrincipal;
import com.busbooking.bus_ticket_management.service.*;
import com.busbooking.bus_ticket_management.utils.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"booking.demo-payments-enabled=true", "booking.expiry-delay-ms=3600000"})
@ActiveProfiles("test")
@Transactional
class Module5IntegrationTest {
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired BusCompanyRepository companies;
    @Autowired BusTripRepository trips;
    @Autowired SeatRepository seats;
    @Autowired SeatHoldRepository holds;
    @Autowired TicketRepository ticketRepo;
    @Autowired PaymentTransactionRepository transactionRepo;
    @Autowired BookingService booking;
    @Autowired TicketService tickets;
    @Autowired PaymentService payments;
    @Autowired SeatLockService locking;
    @Autowired CancellationPolicy cancellation;
    @Autowired PDFExporter pdf;
    @Autowired QRCodeGenerator qr;
    @Autowired EntityManager em;
    User owner, other;
    BusTrip trip;
    Seat seat;
    String holder;

    @BeforeEach
    void fixture() {
        holder = UUID.randomUUID().toString();
        Role role = roles.findByName(Role.CUSTOMER).orElseGet(() -> roles.save(new Role(Role.CUSTOMER)));
        owner = createUser(role);
        other = createUser(role);
        BusCompany company = companies.save(new BusCompany(null, "Nhà xe kiểm thử", "0900000000", "Hà Nội", 4.8));
        trip = new BusTrip();
        trip.setRouteName("Hà Nội - Hải Phòng");
        trip.setDepartureLocation("Hà Nội"); trip.setArrivalLocation("Hải Phòng");
        trip.setDepartureTime(booking.now().plusDays(3));
        trip.setTicketPrice(new BigDecimal("150000")); trip.setBasePrice(new BigDecimal("150000"));
        trip.setTotalSeats(40); trip.setFloorCount(2); trip.setBusCompany(company);
        trips.saveAndFlush(trip);
        seat = seats.saveAndFlush(new Seat(trip, "A01", 1, 1, 1, new BigDecimal("150000")));
        locking.holdSeat(trip.getId(), seat.getId(), holder);
    }

    User createUser(Role role) {
        User u = new User();
        String key = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        u.setUsername(key); u.setEmail(key + "@example.com");
        u.setFullName("Khách kiểm thử"); u.setPassword("test-hash");
        u.setRole(role); u.setEnabled(true); u.setEmailVerified(true);
        return users.saveAndFlush(u);
    }

    BookingRequest request() {
        return new BookingRequest(trip.getId(), List.of(seat.getId()), PaymentMethodType.BANK_TRANSFER,
                UUID.randomUUID().toString());
    }

    Ticket book() { return booking.book(owner.getId(), request(), holder).get(0); }

    @Test
    void savesAndReadsTicketWithRelations() {
        Ticket ticket = book();
        String code = ticket.getTicketCode();
        em.flush(); em.clear();
        var dto = tickets.detail(owner.getId(), code);
        assertEquals("A01", dto.seat());
        assertEquals("Hà Nội - Hải Phòng", dto.route());
        assertEquals(new BigDecimal("150000.00"), dto.total());
        assertEquals(Seat.SeatStatus.BOOKED, seats.findById(seat.getId()).orElseThrow().getSeatStatus());
        assertTrue(holds.findBySeatId(seat.getId()).isEmpty());
    }

    @Test
    void repeatedBookingReturnsSameTicket() {
        var request = request();
        var first = booking.book(owner.getId(), request, holder);
        var second = booking.book(owner.getId(), request, holder);
        assertEquals(first.get(0).getId(), second.get(0).getId());
        assertEquals(1, ticketRepo.findByUser_IdAndBookingKeyOrderByIdAsc(owner.getId(), request.bookingKey()).size());
    }

    @Test
    void rejectsForeignHold() {
        assertThrows(BookingException.class, () -> booking.book(other.getId(), request(), "foreign-session"));
    }

    @Test
    void rejectsAlreadyBookedSeat() {
        book();
        assertThrows(BookingException.class, () -> booking.book(other.getId(), request(), holder));
    }

    @Test
    void rejectsSeatOutsideTrip() {
        var request = new BookingRequest(trip.getId(), List.of(Long.MAX_VALUE), PaymentMethodType.CASH, UUID.randomUUID().toString());
        assertThrows(BookingException.class, () -> booking.book(owner.getId(), request, holder));
    }

    @Test
    void rejectsDuplicateSeatIds() {
        var r = new BookingRequest(trip.getId(), List.of(seat.getId(), seat.getId()), PaymentMethodType.CASH, UUID.randomUUID().toString());
        assertThrows(BookingException.class, () -> booking.book(owner.getId(), r, holder));
    }

    @Test
    void rejectsExpiredHold() {
        holds.findBySeatId(seat.getId()).orElseThrow().setHoldExpiresAt(booking.now().minusMinutes(1));
        assertThrows(BookingException.class, () -> booking.book(owner.getId(), request(), holder));
    }

    @Test
    void savesServerSeatPriceWithoutDiscountOrSurcharge() {
        seat.setPrice(new BigDecimal("175000"));
        Ticket t = book();
        assertEquals(new BigDecimal("175000.00"), t.getTotalAmount());
        seat.setPrice(new BigDecimal("200000"));
        assertEquals(new BigDecimal("175000.00"), t.getTotalAmount());
    }

    @Test
    void failedThenSuccessfulPaymentPreservesBothAttempts() {
        Ticket t = book();
        assertFalse(payments.pay(owner.getId(), t.getId(), false).success());
        assertEquals(PaymentStatus.UNPAID, t.getPaymentStatus());
        assertTrue(payments.pay(owner.getId(), t.getId(), true).success());
        em.flush(); em.clear();
        assertEquals(2, transactionRepo.findByTicket_IdOrderByCreatedAtDesc(t.getId()).size());
        assertEquals(PaymentStatus.PAID, tickets.detail(owner.getId(), t.getTicketCode()).paymentStatus());
    }

    @Test
    void repeatedSuccessfulPaymentDoesNotChargeTwice() {
        Ticket t = book();
        payments.pay(owner.getId(), t.getId(), true);
        payments.pay(owner.getId(), t.getId(), true);
        assertEquals(1, transactionRepo.findByTicket_IdOrderByCreatedAtDesc(t.getId()).size());
    }

    @Test
    void cannotAccessOrPayOtherCustomersTicket() {
        Ticket t = book();
        assertThrows(BookingException.class, () -> tickets.detail(other.getId(), t.getTicketCode()));
        assertThrows(BookingException.class, () -> payments.pay(other.getId(), t.getId(), true));
    }

    @Test
    void paidCancellationCreatesSingleRefundAndReleasesSeat() {
        Ticket t = book(); payments.pay(owner.getId(), t.getId(), true);
        tickets.cancel(owner.getId(), t.getId(), "Đổi lịch trình");
        tickets.cancel(owner.getId(), t.getId(), "Bấm lại");
        em.flush(); em.clear();
        var dto = tickets.detail(owner.getId(), t.getTicketCode());
        assertEquals(TicketStatus.CANCELLED, dto.status());
        assertEquals(PaymentStatus.REFUNDED, dto.paymentStatus());
        assertEquals(new BigDecimal("150000.00"), dto.refundAmount());
        assertEquals(2, transactionRepo.findByTicket_IdOrderByCreatedAtDesc(t.getId()).size());
        assertEquals(Seat.SeatStatus.AVAILABLE, seats.findById(seat.getId()).orElseThrow().getSeatStatus());
    }

    @Test
    void expiredPendingTicketReleasesSeatWithoutRefund() {
        Ticket t = book(); t.setExpiresAt(Instant.now().minusSeconds(5));
        tickets.expire(t.getId());
        assertEquals(TicketStatus.CANCELLED, t.getTicketStatus());
        assertEquals(PaymentStatus.UNPAID, t.getPaymentStatus());
        assertTrue(transactionRepo.findByTicket_IdOrderByCreatedAtDesc(t.getId()).isEmpty());
    }

    @Test
    void checksCancellationPolicyBoundaries() {
        LocalDateTime now = booking.now();
        BigDecimal amount = new BigDecimal("100000");
        assertEquals(new BigDecimal("100000.00"), cancellation.refund(amount, now.plusHours(24), now));
        assertEquals(new BigDecimal("70000.00"), cancellation.refund(amount, now.plusHours(6), now));
        assertFalse(cancellation.canCancel(now.plusHours(6).minusSeconds(1), now));
    }

    @Test
    void exportsVietnamesePdfAndReadableQr() throws Exception {
        Ticket t = book();
        var dto = tickets.detail(owner.getId(), t.getTicketCode());
        byte[] bytes = pdf.export(dto, "http://localhost:8080/user/tickets/" + t.getTicketCode());
        try (var doc = org.apache.pdfbox.Loader.loadPDF(bytes)) {
            assertEquals(1, doc.getNumberOfPages());
            String text = new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
            assertTrue(text.contains("Hà Nội - Hải Phòng"));
            assertTrue(text.contains(t.getTicketCode()));
        }
        byte[] image = qr.png("DEMO-VÉ-001");
        var bitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(
                new com.google.zxing.client.j2se.BufferedImageLuminanceSource(
                        javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(image)))));
        assertEquals("DEMO-VÉ-001", new com.google.zxing.MultiFormatReader().decode(bitmap).getText());
    }

    @Autowired org.springframework.web.context.WebApplicationContext webContext;
    @Autowired AccountSessionService sessions;

    private org.springframework.mock.web.MockHttpSession browserSession(User user) {
        var session = new org.springframework.mock.web.MockHttpSession();
        session.setMaxInactiveInterval(1800);
        session.setAttribute("seatHolderKey", holder);
        var request = new org.springframework.mock.web.MockHttpServletRequest();
        request.setSession(session);
        sessions.register(new AccountPrincipal(user), request);
        return session;
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void rendersCheckoutSuccessHistoryAndDetailPages() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        var principal = new AccountPrincipal(owner);
        var session = browserSession(owner);
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(principal);
        var checkout = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/booking/checkout")
                .param("tripId", trip.getId().toString()).param("seatIds", seat.getId().toString())
                .session(session).with(auth))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(checkout.contains("Xác nhận đặt vé"));
        java.nio.file.Files.writeString(java.nio.file.Path.of("target/module5-checkout.html"), checkout);
        var r = request();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/booking/confirm")
                .param("tripId", r.tripId().toString()).param("seatIds", seat.getId().toString())
                .param("paymentMethod", r.paymentMethod().name()).param("bookingKey", r.bookingKey())
                .session(session).with(auth)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is3xxRedirection());
        Ticket t = ticketRepo.findByUser_IdAndBookingKeyOrderByIdAsc(owner.getId(), r.bookingKey()).get(0);
        for (String url : List.of("/booking/trips", "/booking/success?bookingKey=" + r.bookingKey(),
                "/user/my-tickets", "/user/tickets/" + t.getTicketCode())) {
            var response = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
                    .session(session).with(auth))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andReturn();
            if (url.startsWith("/user/tickets/")) {
                java.nio.file.Files.writeString(java.nio.file.Path.of("target/module5-detail.html"),
                        response.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
            }
        }
    }

    @Test
    void httpEnforcesLoginCsrfAndTicketOwnership() throws Exception {
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        Ticket t = book();
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/user/my-tickets"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is3xxRedirection());
        var ownerSession = browserSession(owner);
        var ownerAuth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(new AccountPrincipal(owner));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/payments/" + t.getTicketCode() + "/simulate")
                .param("success", "true").session(ownerSession).with(ownerAuth))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isForbidden());
        var otherSession = browserSession(other);
        var otherAuth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(new AccountPrincipal(other));
        for (String suffix : List.of("", "/pdf", "/qr", "/payment-qr")) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/user/tickets/" + t.getTicketCode() + suffix)
                    .session(otherSession).with(otherAuth))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        }
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/payments/" + t.getTicketCode() + "/simulate")
                .param("success", "true").session(ownerSession).with(ownerAuth)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf()))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is3xxRedirection());
        assertEquals(PaymentStatus.PAID, tickets.detail(owner.getId(), t.getTicketCode()).paymentStatus());
    }
    @Test
    void recordsSelectedBankForBothFailedAndSuccessfulAttempts() {
        Ticket t = book();
        payments.pay(owner.getId(), t.getId(), false, "BIDV");
        payments.pay(owner.getId(), t.getId(), true, "BIDV");
        em.flush(); em.clear();
        var history = transactionRepo.findByTicket_IdOrderByCreatedAtDesc(t.getId());
        assertEquals(2, history.size());
        assertTrue(history.stream().allMatch(tx -> "BIDV".equals(tx.getPaymentChannel())));
    }

    @Test
    void rejectsWalletProviderOnBankTransferTicket() {
        Ticket t = book();
        assertThrows(BookingException.class, () -> payments.pay(owner.getId(), t.getId(), true, "MOMO"));
        assertTrue(transactionRepo.findByTicket_IdOrderByCreatedAtDesc(t.getId()).isEmpty());
        assertEquals(PaymentStatus.UNPAID, t.getPaymentStatus());
    }

    @Test
    void cashTicketHasNoPaymentQrPanelOrEndpoint() throws Exception {
        var r = new BookingRequest(trip.getId(), List.of(seat.getId()), PaymentMethodType.CASH, UUID.randomUUID().toString());
        Ticket t = booking.book(owner.getId(), r, holder).get(0);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        var session = browserSession(owner);
        var auth = org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(new AccountPrincipal(owner));
        String html = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/user/tickets/" + t.getTicketCode())
                        .session(session).with(auth))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("data-payment-panel=\"CASH\""));
        assertFalse(html.contains("data-payment-qr"));
        assertFalse(html.contains("data-channel-select"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/user/tickets/" + t.getTicketCode() + "/payment-qr")
                        .session(session).with(auth))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
        payments.pay(owner.getId(), t.getId(), true, null);
        assertEquals("CASH", transactionRepo.findByTicket_IdOrderByCreatedAtDesc(t.getId()).get(0).getPaymentChannel());
    }

    @Test
    void walletChoiceIsRecordedAndHasNoBankSelector() throws Exception {
        var r = new BookingRequest(trip.getId(), List.of(seat.getId()), PaymentMethodType.E_WALLET, UUID.randomUUID().toString());
        Ticket t = booking.book(owner.getId(), r, holder).get(0);
        var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(webContext)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity()).build();
        String html = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/user/tickets/" + t.getTicketCode())
                        .session(browserSession(owner))
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user(new AccountPrincipal(owner))))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(html.contains("ZaloPay"));
        assertFalse(html.contains("Vietcombank"));
        payments.pay(owner.getId(), t.getId(), true, "ZALOPAY");
        assertEquals("ZALOPAY", transactionRepo.findByTicket_IdOrderByCreatedAtDesc(t.getId()).get(0).getPaymentChannel());
    }

    @Test
    void vipPriceAndTypeAreSnapshottedWithoutExtraSurcharge() throws Exception {
        seat.setSeatType(SeatType.VIP);
        seat.setBenefits("Giường rộng; nước uống; chăn riêng.");
        seat.setPrice(new BigDecimal("220000"));
        seats.saveAndFlush(seat);
        var layout = locking.getSeatLayout(trip.getId(), holder);
        assertEquals("VIP", layout.getFloors().get(1).get(0).getSeatType());
        assertTrue(layout.getFloors().get(1).get(0).getBenefits().contains("chăn riêng"));
        var preview = booking.preview(owner.getId(), trip.getId(), List.of(seat.getId()), holder);
        assertEquals(SeatType.VIP, preview.seats().get(0).seatType());
        assertEquals(0, new BigDecimal("220000").compareTo(preview.total()));
        var ticket = book();
        payments.pay(owner.getId(), ticket.getId(), true, "BIDV");
        seat.setSeatType(SeatType.NORMAL);
        seat.setBenefits("Thay đổi tiện ích sau đặt vé");
        seat.setPrice(new BigDecimal("999000"));
        em.flush(); em.clear();
        var detail = tickets.detail(owner.getId(), ticket.getTicketCode());
        assertEquals(SeatType.VIP, detail.seatType());
        assertTrue(detail.benefits().contains("chăn riêng"));
        assertEquals(0, new BigDecimal("220000").compareTo(detail.total()));
        byte[] exported = pdf.export(detail, "http://localhost:8081/user/tickets/" + ticket.getTicketCode());
        java.nio.file.Files.write(java.nio.file.Path.of("target/ticket-preview.pdf"), exported);
        try (var document = org.apache.pdfbox.Loader.loadPDF(exported)) {
            String content = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
            assertTrue(content.contains("HẠNG VIP"));
            assertTrue(content.contains("chăn riêng"));
            assertTrue(content.contains("ĐÃ XÁC NHẬN"));
            assertTrue(content.contains("220.000"));
        }
    }
}
