package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.RouteRecommendationDto;
import com.busbooking.bus_ticket_management.dto.SortOption;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.repository.BusTripRepository;
import com.busbooking.bus_ticket_management.utils.CurrencyFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeSet;

/**
 * Gợi ý tuyến phổ biến và dựng trang chi tiết tuyến.
 */
@Service
@Transactional(readOnly = true)
public class RouteService {

    public static final int MAX_LIMIT = 20;
    public static final int DETAIL_TRIP_LIMIT = 8;
    public static final int RELATED_LIMIT = 4;

    private final BusTripRepository tripRepository;
    private final SearchService searchService;
    private final FilterService filterService;

    public RouteService(BusTripRepository tripRepository, SearchService searchService, FilterService filterService) {
        this.tripRepository = tripRepository;
        this.searchService = searchService;
        this.filterService = filterService;
    }

    /**
     * Các tuyến có nhiều chuyến sắp chạy nhất (Native SQL GROUP BY).
     *
     * @param origin nếu có: chỉ lấy các tuyến xuất phát từ điểm này
     * @param limit  số tuyến tối đa (kẹp trong 1..20)
     */
    public List<RouteRecommendationDto> getPopularRoutes(String origin, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        String normalizedOrigin = filterService.normalizeLocation(origin);

        List<RouteRecommendationDto> routes = new ArrayList<>();
        for (Object[] row : tripRepository.findRouteStatistics(normalizedOrigin, filterService.now())) {
            if (routes.size() >= safeLimit) {
                break;
            }
            routes.add(fromStatisticsRow(row));
        }
        applyHighlights(routes);
        return routes;
    }

    /**
     * Chi tiết một tuyến: thống kê, các chuyến sắp chạy, giá theo ngày.
     *
     * @throws IllegalArgumentException khi thiếu/trùng điểm đi - điểm đến
     * @throws NoSuchElementException   khi tuyến chưa có chuyến nào sắp chạy
     */
    public RouteRecommendationDto getRouteDetail(String origin, String destination) {
        String from = filterService.normalizeLocation(origin);
        String to = filterService.normalizeLocation(destination);
        if (from.isEmpty() || to.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng chọn điểm đi và điểm đến.");
        }
        if (from.equalsIgnoreCase(to)) {
            throw new IllegalArgumentException("Điểm đi và điểm đến không được trùng nhau.");
        }

        List<BusTrip> upcoming = tripRepository.findUpcomingByRoute(from, to, filterService.now());
        if (upcoming.isEmpty()) {
            throw new NoSuchElementException(
                    "Chưa có chuyến xe nào sắp chạy trên tuyến " + from + " → " + to + ".");
        }

        RouteRecommendationDto dto = new RouteRecommendationDto();
        dto.setOrigin(upcoming.get(0).getDepartureLocation());
        dto.setDestination(upcoming.get(0).getArrivalLocation());
        dto.setTripCount(upcoming.size());

        BigDecimal min = upcoming.stream().map(BusTrip::getTicketPrice).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal max = upcoming.stream().map(BusTrip::getTicketPrice).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal sum = upcoming.stream().map(BusTrip::getTicketPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = sum.divide(BigDecimal.valueOf(upcoming.size()), 0, RoundingMode.HALF_UP);
        setPrices(dto, min, max, avg);

        double avgRating = upcoming.stream().mapToDouble(SortOption::ratingOf).average().orElse(0);
        dto.setAvgRating(Math.round(avgRating * 10.0) / 10.0);
        dto.setTotalSeats(upcoming.stream().mapToLong(t -> t.getTotalSeats() == null ? 0 : t.getTotalSeats()).sum());

        dto.setBusTypeLabels(filterService.distinctBusTypeLabels(upcoming));
        dto.setCompanies(new ArrayList<>(new TreeSet<>(
                upcoming.stream().map(t -> t.getBusCompany().getName()).toList())));

        dto.setUpcomingTrips(searchService.toDtos(
                upcoming.stream().limit(DETAIL_TRIP_LIMIT).toList(), Map.of()));
        dto.setDailyPrices(searchService.getDailyPrices(dto.getOrigin(), dto.getDestination(),
                filterService.today(), null));
        return dto;
    }

    /** Tuyến liên quan: chiều ngược lại (nếu có) rồi tới các tuyến khác cùng điểm đi. */
    public List<RouteRecommendationDto> getRelatedRoutes(String origin, String destination) {
        String from = filterService.normalizeLocation(origin);
        String to = filterService.normalizeLocation(destination);
        List<RouteRecommendationDto> related = new ArrayList<>();

        for (RouteRecommendationDto route : getPopularRoutes(to, MAX_LIMIT)) {
            if (route.getDestination().equalsIgnoreCase(from)) {
                related.add(route); // chiều về
            }
        }
        for (RouteRecommendationDto route : getPopularRoutes(from, MAX_LIMIT)) {
            if (related.size() >= RELATED_LIMIT) {
                break;
            }
            if (!route.getDestination().equalsIgnoreCase(to)) {
                related.add(route);
            }
        }
        return related;
    }

    // ------------------------------------------------------------------ hàm phụ

    private RouteRecommendationDto fromStatisticsRow(Object[] row) {
        RouteRecommendationDto dto = new RouteRecommendationDto();
        dto.setOrigin((String) row[0]);
        dto.setDestination((String) row[1]);
        dto.setTripCount(((Number) row[2]).longValue());
        setPrices(dto, CurrencyFormatter.toBigDecimal(row[3]), CurrencyFormatter.toBigDecimal(row[4]), null);
        double rating = row[5] == null ? 0 : ((Number) row[5]).doubleValue();
        dto.setAvgRating(Math.round(rating * 10.0) / 10.0);
        dto.setTotalSeats(row[6] == null ? 0 : ((Number) row[6]).longValue());
        return dto;
    }

    private void setPrices(RouteRecommendationDto dto, BigDecimal min, BigDecimal max, BigDecimal avg) {
        dto.setMinPrice(min);
        dto.setMaxPrice(max);
        dto.setMinPriceText(CurrencyFormatter.format(min));
        dto.setMaxPriceText(CurrencyFormatter.format(max));
        if (avg != null) {
            dto.setAvgPrice(avg);
            dto.setAvgPriceText(CurrencyFormatter.format(avg));
        }
    }

    /**
     * Gắn nhãn nổi bật (mỗi nhãn một tuyến): tuyến đứng đầu = "Phổ biến nhất",
     * giá thấp nhất = "Giá tốt nhất", đánh giá cao nhất = "Được đánh giá cao".
     * Chỉ gắn khi có ít nhất 3 tuyến để nhãn có ý nghĩa.
     */
    private void applyHighlights(List<RouteRecommendationDto> routes) {
        if (routes.size() < 3) {
            return;
        }
        routes.get(0).setHighlight("Phổ biến nhất");

        RouteRecommendationDto cheapest = routes.stream()
                .filter(r -> r.getHighlight() == null)
                .min(Comparator.comparing(RouteRecommendationDto::getMinPrice))
                .orElse(null);
        if (cheapest != null) {
            cheapest.setHighlight("Giá tốt nhất");
        }

        RouteRecommendationDto best = routes.stream()
                .filter(r -> r.getHighlight() == null)
                .max(Comparator.comparingDouble(RouteRecommendationDto::getAvgRating))
                .orElse(null);
        if (best != null) {
            best.setHighlight("Được đánh giá cao");
        }
    }
}
