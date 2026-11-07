package com.busbooking.bus_ticket_management;

import com.busbooking.bus_ticket_management.dto.BusTripRequest;
import com.busbooking.bus_ticket_management.dto.BusTripResponse;
import com.busbooking.bus_ticket_management.exception.BusCompanyNotFoundException;
import com.busbooking.bus_ticket_management.exception.BusTripNotFoundException;
import com.busbooking.bus_ticket_management.model.BusCompany;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.repository.BusCompanyRepository;
import com.busbooking.bus_ticket_management.repository.BusTripRepository;
import com.busbooking.bus_ticket_management.service.BusTripService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BusTripServiceUnitTest {

    @Mock
    private BusTripRepository busTripRepository;

    @Mock
    private BusCompanyRepository busCompanyRepository;

    @InjectMocks
    private BusTripService busTripService;

    private BusCompany mockCompany;
    private BusTrip mockTrip;

    @BeforeEach
    void setUp() {
        mockCompany = new BusCompany(1L, "Phương Trang", "0901234567", "TP.HCM", 4.8);
        mockTrip = new BusTrip(100L, "Hà Nội - Đà Nẵng", "Bến xe Giáp Bát", "Bến xe Đà Nẵng",
                LocalDateTime.now().plusDays(1), new BigDecimal("350000"), 40, 2, new BigDecimal("350000"),
                "Giường nằm", mockCompany);
    }

    @Test
    @DisplayName("Lấy tất cả chuyến xe thành công")
    void testGetAllTripsSuccess() {
        when(busTripRepository.findAll()).thenReturn(List.of(mockTrip));

        List<BusTripResponse> responses = busTripService.getAllTrips();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Hà Nội - Đà Nẵng", responses.get(0).getRouteName());
        verify(busTripRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Lấy chi tiết chuyến xe thành công theo ID")
    void testGetTripByIdSuccess() {
        when(busTripRepository.findById(100L)).thenReturn(Optional.of(mockTrip));

        BusTripResponse response = busTripService.getTripById(100L);

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals("Phương Trang", response.getCompanyName());
    }

    @Test
    @DisplayName("Ném lỗi BusTripNotFoundException khi không tìm thấy chuyến xe")
    void testGetTripByIdNotFound() {
        when(busTripRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusTripNotFoundException.class, () -> busTripService.getTripById(999L));
    }

    @Test
    @DisplayName("Tạo chuyến xe mới thành công")
    void testCreateTripSuccess() {
        BusTripRequest request = new BusTripRequest();
        request.setRouteName("Hà Nội - Hải Phòng");
        request.setDepartureLocation("Gia Lâm");
        request.setArrivalLocation("Niệm Nghĩa");
        request.setDepartureTime(LocalDateTime.now().plusDays(2));
        request.setTicketPrice(new BigDecimal("150000"));
        request.setTotalSeats(34);
        request.setBusType("Limousine");
        request.setCompanyId(1L);

        when(busCompanyRepository.findById(1L)).thenReturn(Optional.of(mockCompany));
        when(busTripRepository.save(any(BusTrip.class))).thenReturn(mockTrip);

        assertDoesNotThrow(() -> busTripService.createTrip(request));
        verify(busTripRepository, times(1)).save(any(BusTrip.class));
    }

    @Test
    @DisplayName("Ném lỗi BusCompanyNotFoundException khi tạo chuyến xe với nhà xe không tồn tại")
    void testCreateTripCompanyNotFound() {
        BusTripRequest request = new BusTripRequest();
        request.setCompanyId(999L);

        when(busCompanyRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusCompanyNotFoundException.class, () -> busTripService.createTrip(request));
        verify(busTripRepository, never()).save(any(BusTrip.class));
    }

    @Test
    @DisplayName("Xóa chuyến xe thành công khi ID tồn tại")
    void testDeleteTripSuccess() {
        when(busTripRepository.existsById(100L)).thenReturn(true);
        doNothing().when(busTripRepository).deleteById(100L);

        assertDoesNotThrow(() -> busTripService.deleteTrip(100L));
        verify(busTripRepository, times(1)).deleteById(100L);
    }

    @Test
    @DisplayName("Ném lỗi khi xóa chuyến xe không tồn tại")
    void testDeleteTripNotFound() {
        when(busTripRepository.existsById(999L)).thenReturn(false);

        assertThrows(BusTripNotFoundException.class, () -> busTripService.deleteTrip(999L));
        verify(busTripRepository, never()).deleteById(anyLong());
    }
}