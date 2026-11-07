package com.busbooking.bus_ticket_management.controller;

import com.busbooking.bus_ticket_management.model.BusCompany;
import com.busbooking.bus_ticket_management.service.BusCompanyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/bus-companies")
public class BusCompanyController {

    @Autowired
    private BusCompanyService busCompanyService;

    @GetMapping
    public String listCompanies(Model model) {
        model.addAttribute("companies", busCompanyService.getAllCompanies());
        model.addAttribute("company", new BusCompany());
        return "admin/bus-companies";
    }

    @PostMapping("/save")
    public String saveCompany(@Valid @ModelAttribute("company") BusCompany company,
                              BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("companies", busCompanyService.getAllCompanies());
            return "admin/bus-companies";
        }
        busCompanyService.saveCompany(company);
        return "redirect:/admin/bus-companies";
    }

    @GetMapping("/delete/{id}")
    public String deleteCompany(@PathVariable("id") Long id) {
        busCompanyService.deleteCompany(id);
        return "redirect:/admin/bus-companies";
    }
}