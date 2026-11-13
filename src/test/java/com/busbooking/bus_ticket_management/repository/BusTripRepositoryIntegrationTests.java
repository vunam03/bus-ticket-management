package com.busbooking.bus_ticket_management.repository;

import com.busbooking.bus_ticket_management.dto.BusType;
import com.busbooking.bus_ticket_management.model.BusCompany;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.model.Seat;
import com.busbooking.bus_ticket_management.utils.DateUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chạy các câu JPQL / Native SQL của Module 3 trên H2 (profile "test", chế độ MySQL).
 * Mỗi test chạy trong một transaction rồi tự rollback nên không để lại dữ liệu.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BusTripRepositoryIntegrationTests {

    private static final BigDecimal MIN = BigDecimal.ZERO;
    private static final BigDecimal MAX = new BigDecimal("10000000");
    private static final List<String> NO_TYPE = List.of("__none__");

    @Autowired
    private BusTripRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private LocalDate day;

    private BusCompany company(String name, double rating) {
        BusCompany company = new BusCompany();
        company.setName(name);
        company.setPhoneNumber("0900000000");
        company.setAddress("Hà Nội");
        company.setRating(rating);
        entityManager.persist(company);
        return company;
    }

    private BusTrip save(String from, String to, LocalDateTime departure, long price,
                         String busType, int seats, BusCompany company) {
        BusTrip trip = new BusTrip();
        trip.setRouteName(from + " - " + to);
        trip.setDepartureLocation(from);
        trip.setArrivalLocation(to);
        trip.setDepartureTime(departure);
        trip.setTicketPrice(BigDecimal.valueOf(price));
        trip.setTotalSeats(seats);
        trip.setBasePrice(BigDecimal.valueOf(price));
        trip.setBusType(busType);
        trip.setBusCompany(company);
        return repository.save(trip);
    }

    @BeforeEach
    void setUp() {
        day = LocalDate.now(DateUtils.VN_ZONE).plusDays(1);
        BusCompany top = company("Nhà xe Tốt", 4.8);
        BusCompany low = company("Nhà xe Thường", 3.5);
        save("Hà Nội", "Hải Phòng", day.atTime(6, 0), 150_000, "Ghế ngồi", 45, top);        // A - sáng
        save("Hà Nội", "Hải Phòng", day.atTime(13, 0), 200_000, "Limousine", 22, low);      // B - trưa
        save("Hà Nội", "Hải Phòng", day.atTime(22, 0), 250_000, "Giường nằm", 36, top);    // C - tối
        save("Hà Nội", "Hải Phòng", day.atTime(2, 0), 180_000, "ghe ngoi", 45, low);       // D - rạng sáng (khung Tối), viết không dấu
        save("Hà Nội", "Hải Phòng", day.plusDays(1).atTime(8, 0), 120_000, "Ghế ngồi", 45, top); // E - ngày hôm sau
        save("Hà Nội", "Sa Pa", day.atTime(7, 0), 280_000, "Giường nằm", 36, top);          // F - tuyến khác
        entityManager.flush();
        entityManager.clear();
    }

    private List<BusTrip> search(String origin, String destination, BigDecimal minPrice, BigDecimal maxPrice,
                                 double minRating, List<String> busTypes, boolean anyBusType,
                                 boolean morning, boolean noon, boolean evening, boolean anyTimeSlot) {
        return repository.searchByFilters(origin, destination, day.atStartOfDay(), day.plusDays(1).atStartOfDay(),
                minPrice, maxPrice, minRating, busTypes, anyBusType, morning, noon, evening, anyTimeSlot);
    }

    /** Chỉ lọc tuyến + ngày, mọi bộ lọc khác tắt. */
    private List<BusTrip> searchRoute(String origin, String destination) {
        return search(origin, destination, MIN, MAX, 0, NO_TYPE, true, false, false, false, true);
    }

    private static List<String> hours(List<BusTrip> trips) {
        List<String> hours = new ArrayList<>();
        for (BusTrip t : trips) {
            hours.add(DateUtils.formatTime(t.getDepartureTime()));
        }
        return hours;
    }

    // ---------------------------------------------------------------- searchTrips (JPQL)

    @Test
    void filtersByRouteAndDateSortedByDeparture() {
        assertThat(hours(searchRoute("Hà Nội", "Hải Phòng"))).containsExactly("02:00", "06:00", "13:00", "22:00");
    }

    @Test
    void routeMatchingIgnoresCase() {
        assertThat(searchRoute("hà nội", "HẢI PHÒNG")).hasSize(4);
    }

    @Test
    void emptyOriginAndDestinationMeanAnyRoute() {
        assertThat(searchRoute("", "")).hasSize(5);
        assertThat(searchRoute("", "Sa Pa")).hasSize(1);
    }

    @Test
    void companyIsFetchedTogetherWithTheTrip() {
        // entityManager đã clear() nên nếu không JOIN FETCH thì dòng dưới sẽ lỗi LazyInitialization/N+1
        BusTrip first = searchRoute("Hà Nội", "Hải Phòng").get(0);
        assertThat(first.getBusCompany().getName()).isNotBlank();
    }

    @Test
    void filtersByPriceRangeInclusive() {
        List<BusTrip> result = search("Hà Nội", "Hải Phòng", new BigDecimal("150000"), new BigDecimal("200000"),
                0, NO_TYPE, true, false, false, false, true);
        assertThat(hours(result)).containsExactly("02:00", "06:00", "13:00");
    }

    @Test
    void filtersByBusTypeAndMatchesDifferentSpellings() {
        List<BusTrip> seat = search("Hà Nội", "Hải Phòng", MIN, MAX, 0,
                BusType.GHE_NGOI.getDatabaseValues(), false, false, false, false, true);
        assertThat(hours(seat)).containsExactly("02:00", "06:00"); // "ghe ngoi" và "Ghế ngồi"

        List<String> sleeperOrLimo = new ArrayList<>(BusType.GIUONG_NAM.getDatabaseValues());
        sleeperOrLimo.addAll(BusType.LIMOUSINE.getDatabaseValues());
        List<BusTrip> other = search("Hà Nội", "Hải Phòng", MIN, MAX, 0,
                sleeperOrLimo, false, false, false, false, true);
        assertThat(hours(other)).containsExactly("13:00", "22:00");
    }

    @Test
    void filtersByMorningSlot() {
        assertThat(hours(search("Hà Nội", "Hải Phòng", MIN, MAX, 0, NO_TYPE, true, true, false, false, false)))
                .containsExactly("06:00");
    }

    @Test
    void filtersByNoonSlot() {
        assertThat(hours(search("Hà Nội", "Hải Phòng", MIN, MAX, 0, NO_TYPE, true, false, true, false, false)))
                .containsExactly("13:00");
    }

    @Test
    void eveningSlotWrapsAroundMidnight() {
        assertThat(hours(search("Hà Nội", "Hải Phòng", MIN, MAX, 0, NO_TYPE, true, false, false, true, false)))
                .containsExactly("02:00", "22:00");
    }

    @Test
    void combinesSeveralTimeSlots() {
        assertThat(hours(search("Hà Nội", "Hải Phòng", MIN, MAX, 0, NO_TYPE, true, true, true, false, false)))
                .containsExactly("06:00", "13:00");
    }

    @Test
    void filtersByMinimumCompanyRating() {
        List<BusTrip> result = search("Hà Nội", "Hải Phòng", MIN, MAX, 4.0, NO_TYPE, true, false, false, false, true);
        assertThat(hours(result)).containsExactly("06:00", "22:00"); // chỉ nhà xe 4,8 sao
    }

    @Test
    void combinesAllFiltersWithAnd() {
        List<BusTrip> result = search("Hà Nội", "Hải Phòng", new BigDecimal("100000"), new BigDecimal("190000"), 4.0,
                BusType.GHE_NGOI.getDatabaseValues(), false, true, true, false, false);
        assertThat(hours(result)).containsExactly("06:00");
    }

    // ---------------------------------------------------------------- JPQL phụ

    @Test
    void findsUpcomingTripsOfRoute() {
        List<BusTrip> upcoming = repository.findUpcomingByRoute("hà nội", "hải phòng", LocalDateTime.now());
        assertThat(upcoming).hasSize(5);
        assertThat(upcoming.get(0).getDepartureTime()).isBefore(upcoming.get(4).getDepartureTime());
        assertThat(upcoming.get(0).getBusCompany().getName()).isNotBlank();
    }

    @Test
    void listsDistinctLocations() {
        assertThat(repository.findDistinctOrigins()).containsExactly("Hà Nội");
        assertThat(repository.findDistinctDestinations()).containsExactly("Hải Phòng", "Sa Pa");
        assertThat(repository.findDestinationsByOrigin("hà nội")).containsExactly("Hải Phòng", "Sa Pa");
        assertThat(repository.findDestinationsByOrigin("Đà Lạt")).isEmpty();
    }

    @Test
    void companyWithoutRatingIsStillFoundUnlessAMinimumRatingIsRequested() {
        BusCompany unrated = new BusCompany();
        unrated.setName("Nhà xe mới");
        unrated.setRating(null);
        entityManager.persist(unrated);
        save("Huế", "Đà Nẵng", day.atTime(9, 0), 100_000, "Ghế ngồi", 45, unrated);
        entityManager.flush();
        entityManager.clear();

        assertThat(searchRoute("Huế", "Đà Nẵng")).hasSize(1);
        assertThat(search("Huế", "Đà Nẵng", MIN, MAX, 3.0, NO_TYPE, true, false, false, false, true)).isEmpty();
    }

    // ---------------------------------------------------------------- ghế (Module 4)

    @Test
    void countsOnlyHeldAndBookedSeatsPerTrip() {
        List<BusTrip> trips = searchRoute("Hà Nội", "Hải Phòng");
        BusTrip first = trips.get(0);
        BusTrip second = trips.get(1);

        entityManager.persist(seat(first, "A1", Seat.SeatStatus.BOOKED));
        entityManager.persist(seat(first, "A2", Seat.SeatStatus.HOLD));
        entityManager.persist(seat(first, "A3", Seat.SeatStatus.AVAILABLE));
        entityManager.persist(seat(second, "A1", Seat.SeatStatus.AVAILABLE));
        entityManager.flush();

        List<Object[]> rows = repository.countSeatsNotInStatus(
                List.of(first.getId(), second.getId()), Seat.SeatStatus.AVAILABLE);
        assertThat(rows).hasSize(1); // chuyến 2 chỉ có ghế trống nên không xuất hiện
        assertThat(((Number) rows.get(0)[0]).longValue()).isEqualTo(first.getId());
        assertThat(((Number) rows.get(0)[1]).longValue()).isEqualTo(2L);
    }

    private Seat seat(BusTrip trip, String number, Seat.SeatStatus status) {
        Seat seat = new Seat(trip, number, 1, 0, 0, BigDecimal.valueOf(100_000));
        seat.setSeatStatus(status);
        return seat;
    }

    // ---------------------------------------------------------------- Native SQL

    @Test
    void dailyMinPricesGroupsByDate() {
        List<Object[]> rows = repository.findDailyMinPrices("Hà Nội", "Hải Phòng",
                day.atStartOfDay(), day.plusDays(2).atStartOfDay());
        assertThat(rows).hasSize(2);

        assertThat(DateUtils.toLocalDate(rows.get(0)[0])).isEqualTo(day);
        assertThat(new BigDecimal(rows.get(0)[1].toString()).intValue()).isEqualTo(150_000);
        assertThat(((Number) rows.get(0)[2]).longValue()).isEqualTo(4L);

        assertThat(DateUtils.toLocalDate(rows.get(1)[0])).isEqualTo(day.plusDays(1));
        assertThat(new BigDecimal(rows.get(1)[1].toString()).intValue()).isEqualTo(120_000);
    }

    @Test
    void routeStatisticsAggregatePerRouteMostTripsFirst() {
        List<Object[]> rows = repository.findRouteStatistics("", LocalDateTime.now());
        assertThat(rows).hasSize(2);

        Object[] top = rows.get(0);
        assertThat(top[0]).isEqualTo("Hà Nội");
        assertThat(top[1]).isEqualTo("Hải Phòng");
        assertThat(((Number) top[2]).longValue()).isEqualTo(5L);
        assertThat(new BigDecimal(top[3].toString()).intValue()).isEqualTo(120_000);
        assertThat(new BigDecimal(top[4].toString()).intValue()).isEqualTo(250_000);
        assertThat(((Number) top[5]).doubleValue()).isBetween(4.27, 4.29); // (4,8x3 + 3,5x2) / 5
        assertThat(((Number) top[6]).longValue()).isEqualTo(193L);        // 45+22+36+45+45

        assertThat(rows.get(1)[1]).isEqualTo("Sa Pa");
    }

    @Test
    void routeStatisticsCanBeLimitedToOneOrigin() {
        assertThat(repository.findRouteStatistics("hà nội", LocalDateTime.now())).hasSize(2);
        assertThat(repository.findRouteStatistics("Đà Lạt", LocalDateTime.now())).isEmpty();
    }
}
