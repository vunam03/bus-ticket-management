package com.busbooking.bus_ticket_management.controller;

import com.busbooking.bus_ticket_management.dto.SearchFilterRequest;
import com.busbooking.bus_ticket_management.dto.SearchResponseDto;
import com.busbooking.bus_ticket_management.service.SearchService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;

/**
 * Các endpoint tra cứu chuyến xe:
 * <pre>
 * GET /                    trang chủ (index.html)
 * GET /search              trang kết quả (HTML, render sẵn kết quả ở server)
 * GET /search/filter       JSON - JavaScript gọi (AJAX) mỗi khi đổi bộ lọc/sắp xếp/trang
 * GET /search/locations    JSON - gợi ý điểm đi / điểm đến
 * GET /search/destinations JSON - điểm đến khả dụng theo điểm đi
 * </pre>
 */
@Controller
public class SearchController {

    private static final String BIND_ERROR_MESSAGE =
            "Dữ liệu tìm kiếm không hợp lệ: ngày phải có dạng yyyy-MM-dd và các giá trị số phải đúng định dạng.";

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Trang chủ. NẾU NHÓM ĐÃ CÓ controller khác ánh xạ "/" (ví dụ HomeController) thì XOÁ method này
     * để tránh lỗi "Ambiguous mapping" khi khởi động; index.html vẫn dùng được vì trang tự gọi AJAX.
     */
    @GetMapping("/")
    public String home() {
        return "index";
    }

    /** Trang kết quả tìm kiếm. Lỗi dữ liệu => vẫn hiển thị trang, kèm thông báo đỏ phía trên. */
    @GetMapping("/search")
    public String search(@ModelAttribute("filter") SearchFilterRequest filter,
                         BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("error", BIND_ERROR_MESSAGE);
            model.addAttribute("result", SearchResponseDto.empty());
            return "search-results";
        }
        try {
            model.addAttribute("result", searchService.search(filter));
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("result", SearchResponseDto.empty());
        }
        return "search-results";
    }

    /** API lọc (AJAX): trả JSON, lỗi dữ liệu trả 400 kèm {"error": "..."}. */
    @GetMapping(value = "/search/filter", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Object> filter(@ModelAttribute SearchFilterRequest filter, BindingResult binding) {
        if (binding.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", BIND_ERROR_MESSAGE));
        }
        try {
            return ResponseEntity.ok(searchService.search(filter));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping(value = "/search/locations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, List<String>> locations() {
        return Map.of(
                "origins", searchService.getOrigins(),
                "destinations", searchService.getDestinations());
    }

    @GetMapping(value = "/search/destinations", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public List<String> destinations(@RequestParam(defaultValue = "") String origin) {
        return searchService.getDestinationsFrom(origin);
    }
}
