package com.busbooking.bus_ticket_management.controller;

import com.busbooking.bus_ticket_management.dto.BusTripRequest;
import com.busbooking.bus_ticket_management.dto.BusTripResponse;
import com.busbooking.bus_ticket_management.service.BusTripService;
import com.busbooking.bus_ticket_management.service.BusCompanyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/trips")
public class BusTripController {

    @Autowired
    private BusTripService busTripService;

    @Autowired
    private BusCompanyService busCompanyService;

    @GetMapping
    public String listTrips(Model model) {
        model.addAttribute("trips", busTripService.getAllTrips());
        return "admin/trips";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        model.addAttribute("tripRequest", new BusTripRequest());
        model.addAttribute("companies", busCompanyService.getAllCompanies());
        return "admin/trip-add";
    }

    @PostMapping("/add")
    public String createTrip(@Valid @ModelAttribute("tripRequest") BusTripRequest request,
                             BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("companies", busCompanyService.getAllCompanies());
            return "admin/trip-add";
        }
        busTripService.createTrip(request);
        return "redirect:/admin/trips";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable("id") Long id, Model model) {
        BusTripResponse trip = busTripService.getTripById(id);

        BusTripRequest request = new BusTripRequest();
        request.setRouteName(trip.getRouteName());
        request.setDepartureLocation(trip.getDepartureLocation());
        request.setArrivalLocation(trip.getArrivalLocation());
        request.setDepartureTime(trip.getDepartureTime());
        request.setTicketPrice(trip.getTicketPrice());
        request.setTotalSeats(trip.getTotalSeats());
        request.setBusType(trip.getBusType());
        request.setCompanyId(trip.getCompanyId());

        model.addAttribute("tripId", id);
        model.addAttribute("tripRequest", request);
        model.addAttribute("companies", busCompanyService.getAllCompanies());
        return "admin/trip-edit";
    }

    @PostMapping("/edit/{id}")
    public String updateTrip(@PathVariable("id") Long id,
                             @Valid @ModelAttribute("tripRequest") BusTripRequest request,
                             BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("tripId", id);
            model.addAttribute("companies", busCompanyService.getAllCompanies());
            return "admin/trip-edit";
        }
        busTripService.updateTrip(id, request);
        return "redirect:/admin/trips";
    }

    @GetMapping("/delete/{id}")
    public String deleteTrip(@PathVariable("id") Long id) {
        busTripService.deleteTrip(id);
        return "redirect:/admin/trips";
    }
}