package com.busbooking.bus_ticket_management.controller;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Entry points for account navigation; trip CRUD remains in Module 1. */
@Controller
public class AccountHomeController {
    @GetMapping("/")
    public String home() {
        return "index";
    }

    @GetMapping("/admin/account")
    public String adminAccount() {
        return "admin/account";
    }
}
