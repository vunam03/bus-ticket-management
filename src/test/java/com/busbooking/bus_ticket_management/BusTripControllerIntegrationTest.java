package com.busbooking.bus_ticket_management;

import com.busbooking.bus_ticket_management.controller.BusTripController;
import com.busbooking.bus_ticket_management.dto.BusTripResponse;
import com.busbooking.bus_ticket_management.service.BusCompanyService;
import com.busbooking.bus_ticket_management.service.BusTripService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BusTripController.class)
public class BusTripControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BusTripService busTripService;

    @MockitoBean
    private BusCompanyService busCompanyService;

    @Test
    @DisplayName("Test HTTP GET /admin/trips hiển thị view trips.html thành công")
    void testListTripsView() throws Exception {
        BusTripResponse response = new BusTripResponse();
        response.setId(1L);
        response.setRouteName("Hà Nội - Sài Gòn");
        response.setDepartureLocation("Giáp Bát");
        response.setArrivalLocation("Miền Đông");
        response.setDepartureTime(LocalDateTime.now().plusDays(1));
        response.setTicketPrice(new BigDecimal("800000"));
        response.setTotalSeats(42);
        response.setCompanyName("Hoàng Long");

        given(busTripService.getAllTrips()).willReturn(List.of(response));

        mockMvc.perform(get("/admin/trips"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/trips"))
                .andExpect(model().attributeExists("trips"));
    }

    @Test
    @DisplayName("Test HTTP GET /admin/trips/add mở trang form thêm chuyến")
    void testShowAddForm() throws Exception {
        given(busCompanyService.getAllCompanies()).willReturn(List.of());

        mockMvc.perform(get("/admin/trips/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/trip-add"))
                .andExpect(model().attributeExists("tripRequest"))
                .andExpect(model().attributeExists("companies"));
    }
}