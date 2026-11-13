package com.busbooking.bus_ticket_management.controller;

import com.busbooking.bus_ticket_management.dto.RouteRecommendationDto;
import com.busbooking.bus_ticket_management.service.RouteService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Endpoint về tuyến đường:
 * <pre>
 * GET /routes                         JSON - tuyến phổ biến (?origin=Hà Nội&limit=6)
 * GET /routes/detail?origin=..&destination=..   trang chi tiết tuyến (route-detail.html)
 * </pre>
 */
@Controller
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @GetMapping(value = "/routes", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<RouteRecommendationDto> popularRoutes(@RequestParam(defaultValue = "") String origin,
                                                      @RequestParam(defaultValue = "6") int limit) {
        return routeService.getPopularRoutes(origin, limit);
    }

    @GetMapping("/routes/detail")
    public String routeDetail(@RequestParam(defaultValue = "") String origin,
                              @RequestParam(defaultValue = "") String destination,
                              Model model) {
        try {
            RouteRecommendationDto route = routeService.getRouteDetail(origin, destination);
            model.addAttribute("route", route);
            model.addAttribute("related", routeService.getRelatedRoutes(route.getOrigin(), route.getDestination()));
        } catch (IllegalArgumentException | NoSuchElementException ex) {
            model.addAttribute("error", ex.getMessage());
        }
        model.addAttribute("origin", origin);
        model.addAttribute("destination", destination);
        return "route-detail";
    }
}
