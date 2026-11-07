package com.busbooking.bus_ticket_management.controller;

import com.busbooking.bus_ticket_management.service.BusTripReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.ByteArrayInputStream;

@Controller
@RequestMapping("/admin/reports")
public class ReportController {

    @Autowired
    private BusTripReportService reportService;

    @GetMapping
    public String showReportDashboard(Model model) {
        model.addAttribute("companyReports", reportService.generateCompanySummaryReport());
        return "admin/dashboard-report";
    }

    @GetMapping("/export/trips-excel")
    public ResponseEntity<InputStreamResource> exportTripsToExcel() {
        ByteArrayInputStream in = reportService.exportTripsToCsv();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=danh-sach-chuyen-xe.csv");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(new InputStreamResource(in));
    }
}