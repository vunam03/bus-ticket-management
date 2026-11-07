package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.BusTripResponse;
import com.busbooking.bus_ticket_management.dto.CompanyReportDto;
import com.busbooking.bus_ticket_management.model.BusCompany;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.repository.BusCompanyRepository;
import com.busbooking.bus_ticket_management.repository.BusTripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BusTripReportService {

    @Autowired
    private BusCompanyRepository busCompanyRepository;

    @Autowired
    private BusTripRepository busTripRepository;

    @Autowired
    private BusTripService busTripService;

    public List<CompanyReportDto> generateCompanySummaryReport() {
        List<BusCompany> companies = busCompanyRepository.findAll();
        List<CompanyReportDto> reports = new ArrayList<>();

        for (BusCompany company : companies) {
            CompanyReportDto dto = new CompanyReportDto();
            dto.setCompanyId(company.getId());
            dto.setCompanyName(company.getName());
            dto.setPhoneNumber(company.getPhoneNumber());

            List<BusTrip> trips = busTripRepository.findByBusCompanyId(company.getId());
            dto.setTotalTrips(trips.size());

            if (!trips.isEmpty()) {
                int totalSeats = 0;
                BigDecimal minPrice = trips.get(0).getTicketPrice();
                BigDecimal maxPrice = trips.get(0).getTicketPrice();
                BigDecimal sumPrice = BigDecimal.ZERO;

                for (BusTrip trip : trips) {
                    totalSeats += trip.getTotalSeats();
                    if (trip.getTicketPrice().compareTo(minPrice) < 0) {
                        minPrice = trip.getTicketPrice();
                    }
                    if (trip.getTicketPrice().compareTo(maxPrice) > 0) {
                        maxPrice = trip.getTicketPrice();
                    }
                    sumPrice = sumPrice.add(trip.getTicketPrice());
                }

                dto.setTotalSeatsOffered(totalSeats);
                dto.setMinTicketPrice(minPrice);
                dto.setMaxTicketPrice(maxPrice);
                dto.setAverageTicketPrice(sumPrice.divide(BigDecimal.valueOf(trips.size()), 2, RoundingMode.HALF_UP));
            } else {
                dto.setTotalSeatsOffered(0);
                dto.setMinTicketPrice(BigDecimal.ZERO);
                dto.setMaxTicketPrice(BigDecimal.ZERO);
                dto.setAverageTicketPrice(BigDecimal.ZERO);
            }

            reports.add(dto);
        }

        return reports;
    }

    public ByteArrayInputStream exportTripsToCsv() {
        List<BusTripResponse> trips = busTripService.getAllTrips();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(out);

        // Viết UTF-8 BOM để Excel hiển thị tiếng Việt không bị lỗi font
        out.write(0xEF);
        out.write(0xBB);
        out.write(0xBF);

        writer.println("Mã Chuyến,Tuyến Đường,Nhà Xe,Điểm Đi,Điểm Đến,Thời Gian Xuất Bến,Giá Vé (VND),Tổng Số Ghế,Loại Xe");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        for (BusTripResponse trip : trips) {
            writer.printf("%d,\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%.2f,%d,\"%s\"%n",
                    trip.getId(),
                    escapeCsv(trip.getRouteName()),
                    escapeCsv(trip.getCompanyName()),
                    escapeCsv(trip.getDepartureLocation()),
                    escapeCsv(trip.getArrivalLocation()),
                    trip.getDepartureTime() != null ? trip.getDepartureTime().format(formatter) : "",
                    trip.getTicketPrice(),
                    trip.getTotalSeats(),
                    escapeCsv(trip.getBusType())
            );
        }

        writer.flush();
        return new ByteArrayInputStream(out.toByteArray());
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}