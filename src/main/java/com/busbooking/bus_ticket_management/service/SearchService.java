package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.BusType;
import com.busbooking.bus_ticket_management.dto.SearchFilterRequest;
import com.busbooking.bus_ticket_management.dto.SearchResponseDto;
import com.busbooking.bus_ticket_management.dto.SearchResponseDto.DailyPrice;
import com.busbooking.bus_ticket_management.dto.SearchResponseDto.Facet;
import com.busbooking.bus_ticket_management.dto.SortOption;
import com.busbooking.bus_ticket_management.dto.TimeSlot;
import com.busbooking.bus_ticket_management.dto.TripSearchResultDto;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.model.Seat;
import com.busbooking.bus_ticket_management.repository.BusTripRepository;
import com.busbooking.bus_ticket_management.service.FilterService.PageSlice;
import com.busbooking.bus_ticket_management.service.FilterService.PriceBounds;
import com.busbooking.bus_ticket_management.service.FilterService.SearchCriteria;
import com.busbooking.bus_ticket_management.utils.CurrencyFormatter;
import com.busbooking.bus_ticket_management.utils.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Điều phối luồng tìm kiếm:
 * request -> FilterService (kiểm tra + chuẩn hoá) -> Repository (truy vấn)
 *         -> FilterService (sắp xếp, phân trang, thống kê) -> DTO trả về giao diện.
 */
@Service
@Transactional(readOnly = true)
public class SearchService {

    public static final int DAILY_PRICE_DAYS = 7;
    /** Số ghế trống từ mức này trở xuống thì cảnh báo "sắp hết chỗ". */
    public static final int LOW_SEATS_THRESHOLD = 5;

    private final BusTripRepository tripRepository;
    private final FilterService filterService;

    public SearchService(BusTripRepository tripRepository, FilterService filterService) {
        this.tripRepository = tripRepository;
        this.filterService = filterService;
    }

    /** Tìm kiếm + lọc + sắp xếp + phân trang. Yêu cầu sai => IllegalArgumentException. */
    public SearchResponseDto search(SearchFilterRequest request) {
        SearchCriteria criteria = filterService.buildCriteria(request);
        SortOption sort = SortOption.fromCode(request.getSortBy());

        List<BusTrip> filtered = query(criteria);
        // Tập "gốc" (chỉ tuyến + ngày) dùng để đếm số lượng ở sidebar và tính khoảng giá thanh trượt.
        // Nếu chưa bật bộ lọc nào thì hai tập giống nhau, khỏi truy vấn lần hai.
        List<BusTrip> base = criteria.hasOptionalFilters() ? query(criteria.relaxed()) : filtered;

        List<BusTrip> sorted = filterService.sort(filtered, sort);
        Map<Long, List<String>> badges = filterService.computeBadges(filtered);
        PageSlice<BusTrip> slice = filterService.paginate(sorted, request.getPage(), request.getSize());

        List<TripSearchResultDto> items = toDtos(slice.items(), badges);

        PriceBounds bounds = filterService.priceBounds(base);
        SearchResponseDto response = new SearchResponseDto();
        response.setTrips(items);
        response.setPage(slice.page());
        response.setSize(slice.size());
        response.setTotalItems(slice.totalItems());
        response.setTotalPages(slice.totalPages());
        response.setTotalBeforeFilters(base.size());
        response.setPriceMin(bounds.min());
        response.setPriceMax(bounds.max());
        response.setBusTypeFacets(buildBusTypeFacets(filterService.countByBusType(base)));
        response.setTimeSlotFacets(buildTimeSlotFacets(filterService.countByTimeSlot(base)));
        response.setSortBy(sort.name());
        response.setSortLabel(sort.getLabel());
        response.setTitle(buildTitle(criteria, base));
        response.setDateLabel(DateUtils.fullDateLabel(request.getTravelDate()));
        response.setActiveFilterCount(filterService.countActiveFilters(criteria));
        response.setDailyPrices(getDailyPrices(criteria.origin(), criteria.destination(),
                request.getTravelDate() == null ? filterService.today() : request.getTravelDate(),
                request.getTravelDate()));
        return response;
    }

    private List<BusTrip> query(SearchCriteria c) {
        return tripRepository.searchByFilters(
                c.origin(), c.destination(), c.fromTime(), c.toTime(),
                c.minPrice(), c.maxPrice(), c.minRating(),
                c.busTypes(), c.anyBusType(),
                c.morning(), c.noon(), c.evening(), c.anyTimeSlot());
    }

    // ------------------------------------------------------------------ chuyển entity -> DTO

    /**
     * Chuyển cả danh sách chuyến sang DTO. Số ghế đang giữ/đã đặt của TẤT CẢ các chuyến được lấy
     * bằng đúng một truy vấn (tránh gọi CSDL cho từng chuyến).
     *
     * @param badges nhãn nổi bật theo id chuyến (có thể rỗng)
     */
    public List<TripSearchResultDto> toDtos(List<BusTrip> trips, Map<Long, List<String>> badges) {
        Map<Long, Long> occupied = countOccupiedSeats(trips);
        List<TripSearchResultDto> result = new ArrayList<>();
        for (BusTrip trip : trips) {
            result.add(toDto(trip, badges.getOrDefault(trip.getId(), List.of()),
                    occupied.getOrDefault(trip.getId(), 0L)));
        }
        return result;
    }

    /** Ánh xạ BusTrip -> DTO. Nếu entity đổi tên field, đây là nơi sửa đầu tiên. */
    public TripSearchResultDto toDto(BusTrip trip, List<String> badges, long occupiedSeats) {
        TripSearchResultDto dto = new TripSearchResultDto();
        LocalDateTime departure = trip.getDepartureTime();
        int total = trip.getTotalSeats() == null ? 0 : trip.getTotalSeats();
        int available = (int) Math.max(0, total - occupiedSeats);
        dto.setId(trip.getId());
        dto.setBusCompany(trip.getBusCompany() == null ? "" : trip.getBusCompany().getName());
        dto.setOrigin(trip.getDepartureLocation());
        dto.setDestination(trip.getArrivalLocation());
        dto.setDepartureTime(departure);
        dto.setDepartureTimeText(DateUtils.formatTime(departure));
        dto.setDateText(DateUtils.formatDate(departure));
        dto.setDayOfWeekText(DateUtils.dayOfWeekName(departure.toLocalDate()));
        dto.setPrice(trip.getTicketPrice());
        dto.setPriceText(CurrencyFormatter.format(trip.getTicketPrice()));
        dto.setBusType(trip.getBusType());
        dto.setBusTypeLabel(BusType.labelOf(trip.getBusType()));
        dto.setTimeSlotLabel(TimeSlot.of(departure).getLabel());
        dto.setTotalSeats(total);
        dto.setAvailableSeats(available);
        dto.setSoldOut(available == 0);
        dto.setLowSeats(available > 0 && available <= LOW_SEATS_THRESHOLD);
        dto.setSeatsText(available == 0 ? "Hết chỗ" : "Còn " + available + "/" + total + " ghế");
        dto.setRating(Math.round(SortOption.ratingOf(trip) * 10.0) / 10.0);
        dto.setBadges(new ArrayList<>(badges));
        return dto;
    }

    /**
     * Số ghế đang giữ (HOLD) hoặc đã đặt (BOOKED) theo id chuyến, đọc từ bảng bus_seats của Module 4.
     * Chuyến chưa có dòng ghế nào được coi là chưa ai giữ/đặt (0).
     */
    private Map<Long, Long> countOccupiedSeats(Collection<BusTrip> trips) {
        Map<Long, Long> occupied = new HashMap<>();
        if (trips.isEmpty()) {
            return occupied;
        }
        List<Long> ids = new ArrayList<>();
        for (BusTrip trip : trips) {
            ids.add(trip.getId());
        }
        for (Object[] row : tripRepository.countSeatsNotInStatus(ids, Seat.SeatStatus.AVAILABLE)) {
            occupied.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }
        return occupied;
    }

    // ------------------------------------------------------------------ dải giá theo ngày

    /**
     * Giá thấp nhất mỗi ngày trong 7 ngày quanh ngày "center" (dùng Native SQL).
     * Chưa chọn đủ điểm đi + điểm đến => trả về danh sách rỗng.
     *
     * @param selectedDate ngày đang được chọn (null nếu tìm mọi ngày) - để đánh dấu trên giao diện
     */
    public List<DailyPrice> getDailyPrices(String origin, String destination,
                                           LocalDate center, LocalDate selectedDate) {
        String from = filterService.normalizeLocation(origin);
        String to = filterService.normalizeLocation(destination);
        if (from.isEmpty() || to.isEmpty()) {
            return List.of();
        }

        LocalDate today = filterService.today();
        LocalDate start = center.minusDays(3);
        if (start.isBefore(today)) {
            start = today;
        }
        LocalDateTime fromTime = DateUtils.startOfDay(start);
        if (fromTime.isBefore(filterService.now())) {
            fromTime = filterService.now();
        }
        LocalDateTime toTime = DateUtils.startOfDay(start.plusDays(DAILY_PRICE_DAYS));

        Map<LocalDate, Object[]> rowsByDate = new HashMap<>();
        for (Object[] row : tripRepository.findDailyMinPrices(from, to, fromTime, toTime)) {
            rowsByDate.put(DateUtils.toLocalDate(row[0]), row);
        }

        List<DailyPrice> result = new ArrayList<>();
        for (int i = 0; i < DAILY_PRICE_DAYS; i++) {
            LocalDate day = start.plusDays(i);
            Object[] row = rowsByDate.get(day);
            BigDecimal price = row == null ? null : CurrencyFormatter.toBigDecimal(row[1]);
            long count = row == null ? 0 : ((Number) row[2]).longValue();
            result.add(new DailyPrice(
                    day.toString(),
                    DateUtils.shortDayName(day) + " " + DateUtils.formatShortDate(day),
                    DateUtils.relativeDayLabel(day, today),
                    price,
                    price == null ? "Hết chuyến" : CurrencyFormatter.format(price),
                    count,
                    day.equals(selectedDate)));
        }
        return result;
    }

    // ------------------------------------------------------------------ gợi ý địa điểm

    public List<String> getOrigins() {
        return tripRepository.findDistinctOrigins();
    }

    public List<String> getDestinations() {
        return tripRepository.findDistinctDestinations();
    }

    public List<String> getDestinationsFrom(String origin) {
        String normalized = filterService.normalizeLocation(origin);
        if (normalized.isEmpty()) {
            return getDestinations();
        }
        return tripRepository.findDestinationsByOrigin(normalized);
    }

    // ------------------------------------------------------------------ hàm phụ

    private List<Facet> buildBusTypeFacets(Map<String, Long> counts) {
        List<Facet> facets = new ArrayList<>();
        for (BusType type : BusType.values()) {
            facets.add(new Facet(type.name(), type.getLabel(), "", counts.getOrDefault(type.name(), 0L)));
        }
        return facets;
    }

    private List<Facet> buildTimeSlotFacets(Map<String, Long> counts) {
        List<Facet> facets = new ArrayList<>();
        for (TimeSlot slot : TimeSlot.values()) {
            facets.add(new Facet(slot.name(), slot.getLabel(), slot.getRange(), counts.getOrDefault(slot.name(), 0L)));
        }
        return facets;
    }

    /** "Hà Nội → Hải Phòng"; ưu tiên tên chuẩn lấy từ dữ liệu để đúng hoa/thường và dấu. */
    private String buildTitle(SearchCriteria criteria, List<BusTrip> base) {
        if (criteria.origin().isEmpty() && criteria.destination().isEmpty()) {
            return "Tất cả chuyến xe";
        }
        String origin = criteria.origin();
        String destination = criteria.destination();
        if (!base.isEmpty()) {
            if (!origin.isEmpty()) {
                origin = base.get(0).getDepartureLocation();
            }
            if (!destination.isEmpty()) {
                destination = base.get(0).getArrivalLocation();
            }
        }
        if (origin.isEmpty()) {
            return "Chuyến xe đến " + destination;
        }
        if (destination.isEmpty()) {
            return "Chuyến xe từ " + origin;
        }
        return origin + " → " + destination;
    }
}
