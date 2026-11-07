package com.busbooking.bus_ticket_management;

import com.busbooking.bus_ticket_management.exception.BusCompanyNotFoundException;
import com.busbooking.bus_ticket_management.model.BusCompany;
import com.busbooking.bus_ticket_management.repository.BusCompanyRepository;
import com.busbooking.bus_ticket_management.service.BusCompanyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BusCompanyServiceUnitTest {

    @Mock
    private BusCompanyRepository busCompanyRepository;

    @InjectMocks
    private BusCompanyService busCompanyService;

    private BusCompany mockCompany;

    @BeforeEach
    void setUp() {
        mockCompany = new BusCompany(1L, "Hoàng Long", "02253999999", "Hải Phòng", 4.5);
    }

    @Test
    @DisplayName("Lấy tất cả danh sách nhà xe")
    void testGetAllCompanies() {
        when(busCompanyRepository.findAll()).thenReturn(List.of(mockCompany));

        List<BusCompany> companies = busCompanyService.getAllCompanies();

        assertNotNull(companies);
        assertEquals(1, companies.size());
        assertEquals("Hoàng Long", companies.get(0).getName());
    }

    @Test
    @DisplayName("Lấy nhà xe theo ID thành công")
    void testGetCompanyByIdSuccess() {
        when(busCompanyRepository.findById(1L)).thenReturn(Optional.of(mockCompany));

        BusCompany company = busCompanyService.getCompanyById(1L);

        assertNotNull(company);
        assertEquals("Hoàng Long", company.getName());
    }

    @Test
    @DisplayName("Ném ngoại lệ khi nhà xe không tồn tại")
    void testGetCompanyByIdNotFound() {
        when(busCompanyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusCompanyNotFoundException.class, () -> busCompanyService.getCompanyById(99L));
    }

    @Test
    @DisplayName("Lưu thông tin nhà xe")
    void testSaveCompany() {
        when(busCompanyRepository.save(mockCompany)).thenReturn(mockCompany);

        assertDoesNotThrow(() -> busCompanyService.saveCompany(mockCompany));
        verify(busCompanyRepository, times(1)).save(mockCompany);
    }
}