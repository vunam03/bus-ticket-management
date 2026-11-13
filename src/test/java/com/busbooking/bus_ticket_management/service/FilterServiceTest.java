package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.SearchFilterRequest;
import com.busbooking.bus_ticket_management.dto.SortOption;
import com.busbooking.bus_ticket_management.dto.TimeSlot;
import com.busbooking.bus_ticket_management.model.BusCompany;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.service.FilterService.PageSlice;
import com.busbooking.bus_ticket_management.service.FilterService.PriceBounds;
import com.busbooking.bus_ticket_management.service.FilterService.SearchCriteria;
import com.busbooking.bus_ticket_management.utils.DateUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Đồng hồ cố định: 07/11/2026 10:00 giờ Việt Nam. */
class FilterServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-07T03:00:00Z"), DateUtils.VN_ZONE);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 11, 8);

    private final FilterService service = new FilterService(CLOCK);

    private static BusTrip trip(long id, String time, long price, String busType, double companyRating) {
        BusCompany company = new BusCompany();
        company.setName("Nhà xe " + id);
        company.setRating(companyRating);
        BusTrip t = new BusTrip();
        t.setId(id);
        t.setRouteName("Hà Nội - Hải Phòng");
        t.setDepartureLocation("Hà Nội");
        t.setArrivalLocation("Hải Phòng");
        t.setDepartureTime(LocalDateTime.parse(time));
        t.setTicketPrice(new BigDecimal(price + ".00"));
        t.setTotalSeats(40);
        t.setBusType(busType);
        t.setBusCompany(company);
        return t;
    }

    private static SearchFilterRequest request() {
        SearchFilterRequest r = new SearchFilterRequest();
        r.setOrigin("Hà Nội");
        r.setDestination("Hải Phòng");
        r.setTravelDate(TOMORROW);
        return r;
    }

    private static List<BusTrip> sample() {
        return List.of(
                trip(1, "2026-11-08T06:00", 150000, "Ghế ngồi", 4.2),
                trip(2, "2026-11-08T13:30", 120000, "ghe ngoi", 3.9),
                trip(3, "2026-11-08T22:00", 200000, "Limousine", 4.8),
                trip(4, "2026-11-08T02:30", 180000, "GIUONG_NAM", 4.7));
    }

    private static List<Long> ids(List<BusTrip> trips) {
        List<Long> ids = new ArrayList<>();
        for (BusTrip t : trips) {
            ids.add(t.getId());
        }
        return ids;
    }

    // ---------------------------------------------------------------- validate

    @Test
    void rejectsSameOriginAndDestinationIgnoringCaseAndSpaces() {
        SearchFilterRequest r = request();
        r.setDestination("  hà   nội ");
        assertThrows(IllegalArgumentException.class, () -> service.validate(r));
    }

    @Test
    void rejectsPastDateButAcceptsToday() {
        SearchFilterRequest r = request();
        r.setTravelDate(LocalDate.of(2026, 11, 6));
        assertThrows(IllegalArgumentException.class, () -> service.validate(r));
        r.setTravelDate(LocalDate.of(2026, 11, 7));
        service.validate(r); // không ném lỗi
    }

    @Test
    void rejectsDateBeyondBookingWindow() {
        SearchFilterRequest r = request();
        r.setTravelDate(LocalDate.of(2026, 11, 7).plusDays(FilterService.MAX_ADVANCE_DAYS + 1));
        assertThrows(IllegalArgumentException.class, () -> service.validate(r));
    }

    @Test
    void rejectsBadNumbers() {
        SearchFilterRequest p = request();
        p.setMinPrice(new BigDecimal("500000"));
        p.setMaxPrice(new BigDecimal("100000"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(p));

        SearchFilterRequest n = request();
        n.setMinPrice(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(n));

        SearchFilterRequest rating = request();
        rating.setMinRating(5.5);
        assertThrows(IllegalArgumentException.class, () -> service.validate(rating));
    }

    @Test
    void rejectsUnknownCodes() {
        SearchFilterRequest a = request();
        a.setBusTypes(List.of("XE_BUYT"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(a));

        SearchFilterRequest b = request();
        b.setTimeSlots(List.of("MIDNIGHT"));
        assertThrows(IllegalArgumentException.class, () -> service.validate(b));

        SearchFilterRequest c = request();
        c.setSortBy("RANDOM");
        assertThrows(IllegalArgumentException.class, () -> service.validate(c));
    }

    // ---------------------------------------------------------------- buildCriteria

    @Test
    void defaultsAreFilledSoNothingIsNull() {
        SearchCriteria c = service.buildCriteria(new SearchFilterRequest());
        assertEquals("", c.origin());
        assertEquals("", c.destination());
        assertEquals(0, c.minPrice().compareTo(FilterService.PRICE_FLOOR));
        assertEquals(0, c.maxPrice().compareTo(FilterService.PRICE_CEILING));
        assertTrue(c.anyBusType());
        assertTrue(c.anyTimeSlot());
        assertFalse(c.busTypes().isEmpty()); // luôn có giá trị giữ chỗ để IN (...) hợp lệ
        assertFalse(c.hasOptionalFilters());
    }

    @Test
    void todayStartsFromNowSoDepartedTripsAreExcluded() {
        SearchFilterRequest r = request();
        r.setTravelDate(LocalDate.of(2026, 11, 7));
        SearchCriteria c = service.buildCriteria(r);
        assertEquals(LocalDateTime.of(2026, 11, 7, 10, 0), c.fromTime());
        assertEquals(LocalDateTime.of(2026, 11, 8, 0, 0), c.toTime());
    }

    @Test
    void futureDateCoversWholeDay() {
        SearchCriteria c = service.buildCriteria(request());
        assertEquals(LocalDateTime.of(2026, 11, 8, 0, 0), c.fromTime());
        assertEquals(LocalDateTime.of(2026, 11, 9, 0, 0), c.toTime());
    }

    @Test
    void noDateMeansFromNowForBookingWindow() {
        SearchFilterRequest r = request();
        r.setTravelDate(null);
        SearchCriteria c = service.buildCriteria(r);
        assertEquals(LocalDateTime.of(2026, 11, 7, 10, 0), c.fromTime());
        assertEquals(c.fromTime().plusDays(FilterService.MAX_ADVANCE_DAYS), c.toTime());
    }

    @Test
    void timeSlotAndBusTypeFlags() {
        SearchFilterRequest r = request();
        r.setTimeSlots(List.of("morning", " EVENING "));
        r.setBusTypes(List.of("limousine", "Giường nằm"));
        SearchCriteria c = service.buildCriteria(r);
        assertTrue(c.morning());
        assertFalse(c.noon());
        assertTrue(c.evening());
        assertFalse(c.anyTimeSlot());
        assertFalse(c.anyBusType());
        assertTrue(c.busTypes().contains("limousine"));
        assertTrue(c.busTypes().contains("giường nằm"));
        assertTrue(c.busTypes().contains("giuong_nam"));
        assertTrue(c.hasOptionalFilters());
    }

    @Test
    void selectingEveryOptionEqualsNoFilter() {
        SearchFilterRequest r = request();
        r.setTimeSlots(List.of("MORNING", "NOON", "EVENING"));
        r.setBusTypes(List.of("GIUONG_NAM", "LIMOUSINE", "GHE_NGOI"));
        SearchCriteria c = service.buildCriteria(r);
        assertTrue(c.anyTimeSlot());
        assertTrue(c.anyBusType());
    }

    @Test
    void relaxedKeepsRouteAndDateOnly() {
        SearchFilterRequest r = request();
        r.setMinPrice(new BigDecimal("100000"));
        r.setMinRating(4.0);
        r.setBusTypes(List.of("LIMOUSINE"));
        SearchCriteria relaxed = service.buildCriteria(r).relaxed();
        assertEquals("Hà Nội", relaxed.origin());
        assertEquals("Hải Phòng", relaxed.destination());
        assertFalse(relaxed.hasOptionalFilters());
    }

    @Test
    void countsActiveFilterGroups() {
        SearchFilterRequest r = request();
        r.setMinPrice(new BigDecimal("100000"));
        r.setBusTypes(List.of("LIMOUSINE"));
        r.setMinRating(4.0);
        assertEquals(3, service.countActiveFilters(service.buildCriteria(r)));
    }

    // ---------------------------------------------------------------- sort / paginate

    @Test
    void sortsByPriceAscending() {
        assertEquals(List.of(2L, 1L, 4L, 3L), ids(service.sort(sample(), SortOption.PRICE_ASC)));
    }

    @Test
    void sortsByEarliestDeparture() {
        assertEquals(List.of(4L, 1L, 2L, 3L), ids(service.sort(sample(), SortOption.DEPARTURE_ASC)));
    }

    @Test
    void sortsByCompanyRatingDescending() {
        assertEquals(List.of(3L, 4L, 1L, 2L), ids(service.sort(sample(), SortOption.RATING_DESC)));
    }

    @Test
    void companyWithoutRatingIsTreatedAsZeroAndNeverBreaksSorting() {
        BusTrip noRating = trip(9, "2026-11-08T09:00", 100000, "Ghế ngồi", 4.0);
        noRating.getBusCompany().setRating(null);
        BusTrip rated = trip(8, "2026-11-08T10:00", 300000, "Ghế ngồi", 4.5);

        assertEquals(0.0, SortOption.ratingOf(noRating), 0.0001);
        assertEquals(List.of(8L, 9L), ids(service.sort(List.of(noRating, rated), SortOption.RATING_DESC)));
        service.computeBadges(List.of(noRating, rated)); // không ném NullPointerException
    }

    @Test
    void ratingTieIsBrokenByCheaperPrice() {
        List<BusTrip> tie = List.of(
                trip(1, "2026-11-08T06:00", 200000, "Ghế ngồi", 4.5),
                trip(2, "2026-11-08T07:00", 150000, "Ghế ngồi", 4.5));
        assertEquals(List.of(2L, 1L), ids(service.sort(tie, SortOption.RATING_DESC)));
    }

    @Test
    void sortDoesNotMutateInput() {
        List<BusTrip> original = new ArrayList<>(sample());
        List<BusTrip> before = new ArrayList<>(original);
        service.sort(original, SortOption.PRICE_ASC);
        assertEquals(before, original);
    }

    @Test
    void paginatesAndClampsOutOfRangePages() {
        List<Integer> numbers = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            numbers.add(i);
        }
        PageSlice<Integer> p1 = service.paginate(numbers, 1, 10);
        assertEquals(10, p1.items().size());
        assertEquals(3, p1.totalPages());
        assertEquals(25, p1.totalItems());

        assertEquals(5, service.paginate(numbers, 3, 10).items().size());
        assertEquals(3, service.paginate(numbers, 99, 10).page());

        PageSlice<Integer> tooLow = service.paginate(numbers, 0, null);
        assertEquals(1, tooLow.page());
        assertEquals(FilterService.DEFAULT_PAGE_SIZE, tooLow.size());

        assertEquals(FilterService.MAX_PAGE_SIZE, service.paginate(numbers, 1, 1000).size());
    }

    @Test
    void emptyListStillHasOnePage() {
        PageSlice<Integer> empty = service.paginate(List.of(), 1, 10);
        assertEquals(1, empty.totalPages());
        assertEquals(0, empty.items().size());
    }

    // ---------------------------------------------------------------- thống kê

    @Test
    void countsByTimeSlotIncludingAfterMidnightTrips() {
        Map<String, Long> counts = service.countByTimeSlot(sample());
        assertEquals(1L, counts.get(TimeSlot.MORNING.name()).longValue());   // 06:00
        assertEquals(1L, counts.get(TimeSlot.NOON.name()).longValue());      // 13:30
        assertEquals(2L, counts.get(TimeSlot.EVENING.name()).longValue());   // 22:00 và 02:30
    }

    @Test
    void countsByBusTypeRecognisesDifferentSpellings() {
        Map<String, Long> counts = service.countByBusType(sample());
        assertEquals(3, counts.size());
        assertEquals(2L, counts.get("GHE_NGOI").longValue());     // "Ghế ngồi" + "ghe ngoi"
        assertEquals(1L, counts.get("LIMOUSINE").longValue());
        assertEquals(1L, counts.get("GIUONG_NAM").longValue());
    }

    @Test
    void unknownBusTypeIsIgnoredInCounts() {
        Map<String, Long> counts = service.countByBusType(
                List.of(trip(1, "2026-11-08T06:00", 100000, "Xe buýt", 4.0)));
        assertEquals(0L, counts.get("GHE_NGOI").longValue());
        assertEquals(0L, counts.get("LIMOUSINE").longValue());
    }

    @Test
    void priceBoundsRoundOutwardToTenThousand() {
        List<BusTrip> trips = List.of(
                trip(1, "2026-11-08T06:00", 123500, "Ghế ngồi", 4.0),
                trip(2, "2026-11-08T07:00", 187000, "Ghế ngồi", 4.0));
        PriceBounds b = service.priceBounds(trips);
        assertEquals(0, b.min().compareTo(new BigDecimal("120000")));
        assertEquals(0, b.max().compareTo(new BigDecimal("190000")));
    }

    @Test
    void priceBoundsNeverCollapseToASinglePoint() {
        PriceBounds b = service.priceBounds(List.of(trip(1, "2026-11-08T06:00", 150000, "Ghế ngồi", 4.0)));
        assertTrue(b.max().compareTo(b.min()) > 0);
    }

    @Test
    void badgesMarkCheapestEarliestAndBestRated() {
        Map<Long, List<String>> badges = service.computeBadges(sample());
        assertTrue(badges.get(2L).contains("Giá tốt nhất"));
        assertTrue(badges.get(4L).contains("Khởi hành sớm nhất"));
        assertTrue(badges.get(3L).contains("Nhà xe đánh giá cao nhất"));
    }

    @Test
    void noBadgeWhenAllTripsAreIdenticalOnThatCriterion() {
        List<BusTrip> same = List.of(
                trip(1, "2026-11-08T06:00", 150000, "Ghế ngồi", 4.5),
                trip(2, "2026-11-08T06:00", 150000, "Ghế ngồi", 4.5));
        assertTrue(service.computeBadges(same).isEmpty());
    }

    @Test
    void noBadgesForSingleTrip() {
        assertTrue(service.computeBadges(List.of(sample().get(0))).isEmpty());
    }

    @Test
    void distinctBusTypeLabelsFollowEnumOrder() {
        assertEquals(List.of("Giường nằm", "Limousine", "Ghế ngồi"), service.distinctBusTypeLabels(sample()));
    }
}
