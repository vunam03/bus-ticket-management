package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.BusTripRequest;
import com.busbooking.bus_ticket_management.dto.BusTripResponse;
import com.busbooking.bus_ticket_management.model.BusCompany;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.repository.BusCompanyRepository;
import com.busbooking.bus_ticket_management.repository.BusTripRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BusTripService {

    @Autowired
    private BusTripRepository busTripRepository;

    @Autowired
    private BusCompanyRepository busCompanyRepository;

    public List<BusTripResponse> getAllTrips() {
        return busTripRepository.findAll().stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    public BusTripResponse getTripById(Long id) {
        BusTrip trip = busTripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến xe với ID: " + id));
        return convertToResponse(trip);
    }

    @Transactional
    public void createTrip(BusTripRequest request) {
        BusCompany company = busCompanyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà xe với ID: " + request.getCompanyId()));

        BusTrip trip = new BusTrip();
        trip.setRouteName(request.getRouteName());
        trip.setDepartureLocation(request.getDepartureLocation());
        trip.setArrivalLocation(request.getArrivalLocation());
        trip.setDepartureTime(request.getDepartureTime());
        trip.setTicketPrice(request.getTicketPrice());
        trip.setTotalSeats(request.getTotalSeats());
        trip.setBusType(request.getBusType());
        trip.setBusCompany(company);
        trip.setFloorCount(2);
        trip.setBasePrice(request.getTicketPrice());

        busTripRepository.save(trip);
    }

    @Transactional
    public void updateTrip(Long id, BusTripRequest request) {
        BusTrip trip = busTripRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chuyến xe với ID: " + id));

        BusCompany company = busCompanyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà xe với ID: " + request.getCompanyId()));

        trip.setRouteName(request.getRouteName());
        trip.setDepartureLocation(request.getDepartureLocation());
        trip.setArrivalLocation(request.getArrivalLocation());
        trip.setDepartureTime(request.getDepartureTime());
        trip.setTicketPrice(request.getTicketPrice());
        trip.setTotalSeats(request.getTotalSeats());
        trip.setBusType(request.getBusType());
        trip.setBusCompany(company);
        trip.setBasePrice(request.getTicketPrice());

        busTripRepository.save(trip);
    }

    @Transactional
    public void deleteTrip(Long id) {
        if (!busTripRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy chuyến xe để xóa!");
        }
        busTripRepository.deleteById(id);
    }

    private BusTripResponse convertToResponse(BusTrip trip) {
        BusTripResponse response = new BusTripResponse();
        response.setId(trip.getId());
        response.setRouteName(trip.getRouteName());
        response.setDepartureLocation(trip.getDepartureLocation());
        response.setArrivalLocation(trip.getArrivalLocation());
        response.setDepartureTime(trip.getDepartureTime());
        response.setTicketPrice(trip.getTicketPrice());
        response.setTotalSeats(trip.getTotalSeats());
        response.setBusType(trip.getBusType());
        if (trip.getBusCompany() != null) {
            response.setCompanyId(trip.getBusCompany().getId());
            response.setCompanyName(trip.getBusCompany().getName());
        }
        return response;
    }
}