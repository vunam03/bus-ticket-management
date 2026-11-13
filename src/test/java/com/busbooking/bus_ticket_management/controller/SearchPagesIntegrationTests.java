package com.busbooking.bus_ticket_management.controller;

import com.busbooking.bus_ticket_management.model.BusCompany;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.utils.DateUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Kiểm tra từ HTTP xuống CSDL: Spring Security + Controller + Service + Repository + Thymeleaf.
 * Khách chưa đăng nhập phải xem được các trang tìm kiếm (SecurityConfig cho phép anyRequest).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SearchPagesIntegrationTests {

    @Autowired
    private WebApplicationContext context;

    @PersistenceContext
    private EntityManager entityManager;

    private MockMvc mvc;
    private String tomorrow;

    private BusTrip save(String from, String to, LocalDateTime departure, long price, String busType, BusCompany company) {
        BusTrip trip = new BusTrip();
        trip.setRouteName(from + " - " + to);
        trip.setDepartureLocation(from);
        trip.setArrivalLocation(to);
        trip.setDepartureTime(departure);
        trip.setTicketPrice(BigDecimal.valueOf(price));
        trip.setTotalSeats(40);
        trip.setBasePrice(BigDecimal.valueOf(price));
        trip.setBusType(busType);
        trip.setBusCompany(company);
        entityManager.persist(trip);
        return trip;
    }

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        LocalDate day = LocalDate.now(DateUtils.VN_ZONE).plusDays(1);
        tomorrow = day.toString();
        BusCompany company = new BusCompany();
        company.setName("Phương Trang");
        company.setRating(4.6);
        entityManager.persist(company);
        save("Hà Nội", "Hải Phòng", day.atTime(6, 0), 150_000, "Ghế ngồi", company);
        save("Hà Nội", "Hải Phòng", day.atTime(22, 0), 250_000, "Giường nằm", company);
        save("Hà Nội", "Sa Pa", day.atTime(7, 0), 280_000, "Giường nằm", company);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void homePageIsPublic() throws Exception {
        String html = mvc.perform(get("/")).andExpect(status().isOk()).andExpect(view().name("index"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Tìm chuyến xe khách phù hợp");
    }

    @Test
    void filterEndpointReturnsMatchingTripsAsJson() throws Exception {
        mvc.perform(get("/search/filter")
                        .param("origin", "Hà Nội").param("destination", "Hải Phòng")
                        .param("travelDate", tomorrow).param("sortBy", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.trips[0].priceText").value("150.000đ"))
                .andExpect(jsonPath("$.trips[0].busCompany").value("Phương Trang"))
                .andExpect(jsonPath("$.trips[1].priceText").value("250.000đ"))
                .andExpect(jsonPath("$.dailyPrices.length()").value(7));
    }

    @Test
    void filterEndpointAppliesBusTypeAndTimeSlot() throws Exception {
        mvc.perform(get("/search/filter")
                        .param("origin", "Hà Nội").param("destination", "Hải Phòng").param("travelDate", tomorrow)
                        .param("busTypes", "GIUONG_NAM").param("timeSlots", "EVENING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.trips[0].departureTimeText").value("22:00"))
                .andExpect(jsonPath("$.totalBeforeFilters").value(2));
    }

    @Test
    void filterEndpointReturns400ForInvalidInput() throws Exception {
        mvc.perform(get("/search/filter").param("origin", "Hà Nội").param("destination", "hà nội"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Điểm đi và điểm đến không được trùng nhau."));
        mvc.perform(get("/search/filter").param("travelDate", "không-phải-ngày"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void searchPageRendersResultsOnTheServer() throws Exception {
        String html = mvc.perform(get("/search")
                        .param("origin", "Hà Nội").param("destination", "Hải Phòng").param("travelDate", tomorrow))
                .andExpect(status().isOk()).andExpect(view().name("search-results"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Phương Trang").contains("150.000đ").contains("Hà Nội → Hải Phòng")
                .contains("Còn 40/40 ghế")
                .containsPattern("/booking/trips/\\d+/seats"); // link sang trang chọn ghế của Module 4
    }

    @Test
    void searchPageShowsMessageInsteadOfFailing() throws Exception {
        String html = mvc.perform(get("/search").param("origin", "A").param("destination", "A"))
                .andExpect(status().isOk()).andExpect(view().name("search-results"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("không được trùng nhau");
    }

    @Test
    void locationEndpointsListWhatExistsInTheDatabase() throws Exception {
        mvc.perform(get("/search/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origins[0]").value("Hà Nội"));
        mvc.perform(get("/search/destinations").param("origin", "Hà Nội"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("Hải Phòng"))
                .andExpect(jsonPath("$[1]").value("Sa Pa"));
    }

    @Test
    void popularRoutesEndpointAggregatesRoutes() throws Exception {
        mvc.perform(get("/routes").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].origin").value("Hà Nội"))
                .andExpect(jsonPath("$[0].destination").value("Hải Phòng"))
                .andExpect(jsonPath("$[0].tripCount").value(2))
                .andExpect(jsonPath("$[0].routeTitle").value("Hà Nội → Hải Phòng"));
    }

    @Test
    void routeDetailPageRendersAndHandlesUnknownRoute() throws Exception {
        String html = mvc.perform(get("/routes/detail").param("origin", "Hà Nội").param("destination", "Hải Phòng"))
                .andExpect(status().isOk()).andExpect(view().name("route-detail"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("Hà Nội → Hải Phòng").contains("150.000đ");

        String missing = mvc.perform(get("/routes/detail").param("origin", "Hà Nội").param("destination", "Cà Mau"))
                .andExpect(status().isOk()).andExpect(view().name("route-detail"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(missing).contains("Chưa có chuyến xe nào");
    }
}
