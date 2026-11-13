package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.SearchFilterRequest;
import com.busbooking.bus_ticket_management.dto.SearchResponseDto;
import com.busbooking.bus_ticket_management.dto.TripSearchResultDto;
import com.busbooking.bus_ticket_management.model.BusCompany;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.repository.BusTripRepository;
import com.busbooking.bus_ticket_management.utils.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Kiểm tra luồng SearchService bằng repository giả (Mockito), đồng hồ cố định 07/11/2026 10:00. */
@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-07T03:00:00Z"), DateUtils.VN_ZONE);
    private static final LocalDate TOMORROW = LocalDate.of(2026, 11, 8);

    @Mock
    private BusTripRepository repository;

    private SearchService service;

    @BeforeEach
    void setUp() {
        service = new SearchService(repository, new FilterService(CLOCK));
    }

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
        t.setTotalSeats(45);
        t.setBusType(busType);
        t.setBusCompany(company);
        return t;
    }

    private static List<BusTrip> sample() {
        return List.of(
                trip(1, "2026-11-08T06:00", 150000, "Ghế ngồi", 4.2),
                trip(2, "2026-11-08T13:30", 120000, "Ghế ngồi", 3.9),
                trip(3, "2026-11-08T22:00", 200000, "Limousine", 4.8),
                trip(4, "2026-11-08T02:30", 180000, "Giường nằm", 4.7));
    }

    /** 13 tham số của BusTripRepository#searchTrips. */
    private void stubSearch(List<BusTrip> result) {
        when(repository.searchByFilters(any(), any(), any(), any(), any(), any(), anyDouble(),
                any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(result);
    }

    private void verifySearchCalls(int times) {
        verify(repository, times(times)).searchByFilters(any(), any(), any(), any(), any(), any(), anyDouble(),
                any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    private static SearchFilterRequest request() {
        SearchFilterRequest r = new SearchFilterRequest();
        r.setOrigin("  Hà   Nội ");
        r.setDestination("Hải Phòng");
        r.setTravelDate(TOMORROW);
        return r;
    }

    private static TripSearchResultDto byId(SearchResponseDto response, long id) {
        for (TripSearchResultDto t : response.getTrips()) {
            if (t.getId() == id) {
                return t;
            }
        }
        throw new AssertionError("Không thấy chuyến " + id);
    }

    private static List<Long> ids(SearchResponseDto response) {
        List<Long> ids = new ArrayList<>();
        for (TripSearchResultDto t : response.getTrips()) {
            ids.add(t.getId());
        }
        return ids;
    }

    @Test
    void sortsByPriceByDefault() {
        stubSearch(sample());
        SearchResponseDto response = service.search(request());
        assertEquals(List.of(2L, 1L, 4L, 3L), ids(response));
        assertEquals(4, response.getTotalItems());
        assertEquals("Hà Nội → Hải Phòng", response.getTitle());
    }

    @Test
    void sortsByDepartureAndRating() {
        stubSearch(sample());
        SearchFilterRequest r = request();
        r.setSortBy("DEPARTURE_ASC");
        assertEquals(List.of(4L, 1L, 2L, 3L), ids(service.search(r)));
        r.setSortBy("RATING_DESC");
        assertEquals(List.of(3L, 4L, 1L, 2L), ids(service.search(r)));
    }

    @Test
    void normalizesLocationBeforeQuerying() {
        stubSearch(sample());
        service.search(request());
        verify(repository).searchByFilters(eq("Hà Nội"), eq("Hải Phòng"), any(), any(), any(), any(), anyDouble(),
                any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
    }

    @Test
    void queriesOnceWhenNoSidebarFilterIsActive() {
        stubSearch(sample());
        service.search(request());
        verifySearchCalls(1);
    }

    @Test
    void queriesTwiceWhenFiltersAreActiveToKeepFacetCountsStable() {
        stubSearch(sample());
        SearchFilterRequest r = request();
        r.setBusTypes(List.of("LIMOUSINE"));
        SearchResponseDto response = service.search(r);
        verifySearchCalls(2);
        assertEquals(1, response.getActiveFilterCount());
    }

    @Test
    void paginatesResults() {
        stubSearch(sample());
        SearchFilterRequest r = request();
        r.setSize(3);
        r.setPage(2);
        SearchResponseDto response = service.search(r);
        assertEquals(2, response.getPage());
        assertEquals(2, response.getTotalPages());
        assertEquals(1, response.getTrips().size());
        assertEquals(4, response.getTotalItems());
    }

    @Test
    void mapsTripToDisplayDto() {
        stubSearch(sample());
        TripSearchResultDto dto = service.search(request()).getTrips().get(0); // chuyến 2: rẻ nhất
        assertEquals("Nhà xe 2", dto.getBusCompany());
        assertEquals("Hà Nội", dto.getOrigin());
        assertEquals("Hải Phòng", dto.getDestination());
        assertEquals("13:30", dto.getDepartureTimeText());
        assertEquals("08/11/2026", dto.getDateText());
        assertEquals("Chủ nhật", dto.getDayOfWeekText());
        assertEquals("120.000đ", dto.getPriceText());
        assertEquals("Ghế ngồi", dto.getBusTypeLabel());
        assertEquals("Trưa", dto.getTimeSlotLabel());
        assertEquals("Còn 45/45 ghế", dto.getSeatsText());
        assertEquals(45, dto.getAvailableSeats());
        assertEquals(3.9, dto.getRating(), 0.001);
        assertTrue(dto.getBadges().contains("Giá tốt nhất"));
    }

    @Test
    void availableSeatsAreTotalMinusHeldAndBookedSeats() {
        stubSearch(sample());
        List<Object[]> occupied = new ArrayList<>();
        occupied.add(new Object[]{1L, 20L});   // chuyến 1: 45 - 20 = 25 chỗ
        occupied.add(new Object[]{2L, 42L});   // chuyến 2: 45 - 42 = 3 chỗ (sắp hết)
        occupied.add(new Object[]{4L, 45L});   // chuyến 4: hết chỗ
        when(repository.countSeatsNotInStatus(any(), any())).thenReturn(occupied);

        SearchResponseDto response = service.search(request());
        TripSearchResultDto t1 = byId(response, 1L);
        TripSearchResultDto t2 = byId(response, 2L);
        TripSearchResultDto t4 = byId(response, 4L);

        assertEquals(25, t1.getAvailableSeats());
        assertEquals("Còn 25/45 ghế", t1.getSeatsText());
        assertTrue(!t1.isLowSeats() && !t1.isSoldOut());

        assertEquals(3, t2.getAvailableSeats());
        assertTrue(t2.isLowSeats());
        assertTrue(!t2.isSoldOut());

        assertEquals(0, t4.getAvailableSeats());
        assertTrue(t4.isSoldOut());
        assertEquals("Hết chỗ", t4.getSeatsText());
    }

    @Test
    void occupiedSeatsAreQueriedOncePerPageNotPerTrip() {
        stubSearch(sample());
        service.search(request());
        verify(repository, times(1)).countSeatsNotInStatus(any(), any());
    }

    @Test
    void priceBoundsAndFacetsComeFromResults() {
        stubSearch(sample());
        SearchResponseDto response = service.search(request());
        assertEquals(0, response.getPriceMin().compareTo(new BigDecimal("120000")));
        assertEquals(0, response.getPriceMax().compareTo(new BigDecimal("200000")));
        assertEquals(3, response.getBusTypeFacets().size());
        assertEquals(3, response.getTimeSlotFacets().size());
        assertEquals(2, response.getTimeSlotFacets().get(2).getCount()); // Tối: 22:00 và 02:30
        assertEquals(2, response.getBusTypeFacets().get(2).getCount());  // Ghế ngồi: 2 chuyến
    }

    @Test
    void emptyResultIsStillValid() {
        stubSearch(List.of());
        SearchResponseDto response = service.search(request());
        assertEquals(0, response.getTotalItems());
        assertEquals(1, response.getTotalPages());
        assertTrue(response.getTrips().isEmpty());
    }

    @Test
    void dailyPricesFillMissingDaysAndMarkSelectedDate() {
        stubSearch(sample());
        List<Object[]> rows = new ArrayList<>();
        rows.add(new Object[]{java.sql.Date.valueOf(TOMORROW), new BigDecimal("120000.00"), 2L});
        when(repository.findDailyMinPrices(any(), any(), any(), any())).thenReturn(rows);

        SearchResponseDto response = service.search(request());
        assertEquals(SearchService.DAILY_PRICE_DAYS, response.getDailyPrices().size());
        assertEquals("2026-11-07", response.getDailyPrices().get(0).getDate()); // không lùi về quá khứ
        assertEquals("Hôm nay", response.getDailyPrices().get(0).getRelative());
        assertEquals("Hết chuyến", response.getDailyPrices().get(0).getPriceText());
        assertEquals("120.000đ", response.getDailyPrices().get(1).getPriceText());
        assertTrue(response.getDailyPrices().get(1).isSelected());
    }

    @Test
    void invalidRequestNeverReachesTheDatabase() {
        SearchFilterRequest r = request();
        r.setDestination("Hà Nội");
        assertThrows(IllegalArgumentException.class, () -> service.search(r));
        verify(repository, never()).searchByFilters(any(), any(), any(), any(), any(), any(), anyDouble(),
                any(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
    }
}
