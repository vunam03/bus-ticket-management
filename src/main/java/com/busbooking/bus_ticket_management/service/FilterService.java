package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.dto.BusType;
import com.busbooking.bus_ticket_management.dto.SearchFilterRequest;
import com.busbooking.bus_ticket_management.dto.SortOption;
import com.busbooking.bus_ticket_management.dto.TimeSlot;
import com.busbooking.bus_ticket_management.model.BusTrip;
import com.busbooking.bus_ticket_management.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Xử lý "luật" của bộ lọc, tách khỏi việc truy vấn CSDL:
 * 1) kiểm tra yêu cầu hợp lệ,  2) chuẩn hoá yêu cầu thành SearchCriteria (không còn null),
 * 3) sắp xếp,  4) phân trang,  5) tính số lượng cho sidebar, khoảng giá và nhãn nổi bật.
 * <p>
 * Dùng Clock thay cho LocalDateTime.now() để unit test có thể "đóng băng" thời gian.
 * (SecurityConfig của nhóm có sẵn một bean Clock UTC; lớp này cố ý dùng đồng hồ giờ Việt Nam riêng
 * vì thời gian khởi hành trong CSDL là giờ địa phương.)
 */
@Service
public class FilterService {

    public static final BigDecimal PRICE_FLOOR = BigDecimal.ZERO;
    public static final BigDecimal PRICE_CEILING = new BigDecimal("10000000");
    public static final BigDecimal PRICE_STEP = new BigDecimal("10000");
    public static final int MAX_ADVANCE_DAYS = 60;
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;

    /** Giá trị giữ chỗ để mệnh đề IN (...) không bao giờ nhận danh sách rỗng. */
    private static final String NO_BUS_TYPE = "__none__";

    private final Clock clock;

    @Autowired
    public FilterService() {
        this(Clock.system(DateUtils.VN_ZONE));
    }

    public FilterService(Clock clock) {
        this.clock = clock;
    }

    // ------------------------------------------------------------------ kiểu dữ liệu trả về

    /** Điều kiện tìm kiếm đã chuẩn hoá: mọi trường đều có giá trị, sẵn sàng truyền xuống repository. */
    public record SearchCriteria(String origin, String destination,
                                 LocalDateTime fromTime, LocalDateTime toTime,
                                 BigDecimal minPrice, BigDecimal maxPrice, double minRating,
                                 List<String> busTypes, boolean anyBusType,
                                 boolean morning, boolean noon, boolean evening, boolean anyTimeSlot) {

        /** Có bộ lọc sidebar nào đang bật không (giá, loại xe, khung giờ, đánh giá). */
        public boolean hasOptionalFilters() {
            return minPrice.compareTo(PRICE_FLOOR) > 0
                    || maxPrice.compareTo(PRICE_CEILING) < 0
                    || minRating > 0
                    || !anyBusType
                    || !anyTimeSlot;
        }

        /** Bản sao chỉ giữ tuyến + ngày, bỏ bộ lọc sidebar (để đếm số lượng cho từng ô lọc). */
        public SearchCriteria relaxed() {
            return new SearchCriteria(origin, destination, fromTime, toTime,
                    PRICE_FLOOR, PRICE_CEILING, 0, List.of(NO_BUS_TYPE), true,
                    false, false, false, true);
        }
    }

    /** Một trang kết quả. */
    public record PageSlice<T>(List<T> items, int page, int size, int totalItems, int totalPages) {
    }

    /** Khoảng giá làm tròn theo bước 10.000đ cho thanh trượt. */
    public record PriceBounds(BigDecimal min, BigDecimal max) {
    }

    // ------------------------------------------------------------------ thời gian

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    // ------------------------------------------------------------------ kiểm tra + chuẩn hoá

    /** Cắt khoảng trắng đầu/cuối, gộp nhiều khoảng trắng liên tiếp; null => chuỗi rỗng. */
    public String normalizeLocation(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    /**
     * Kiểm tra yêu cầu. Sai => ném IllegalArgumentException với thông báo tiếng Việt
     * để Controller hiển thị trực tiếp cho người dùng.
     */
    public void validate(SearchFilterRequest request) {
        String origin = normalizeLocation(request.getOrigin());
        String destination = normalizeLocation(request.getDestination());
        if (!origin.isEmpty() && origin.equalsIgnoreCase(destination)) {
            throw new IllegalArgumentException("Điểm đi và điểm đến không được trùng nhau.");
        }

        LocalDate travelDate = request.getTravelDate();
        if (travelDate != null) {
            if (travelDate.isBefore(today())) {
                throw new IllegalArgumentException("Ngày đi không được ở trong quá khứ.");
            }
            if (travelDate.isAfter(today().plusDays(MAX_ADVANCE_DAYS))) {
                throw new IllegalArgumentException(
                        "Chỉ hỗ trợ tìm chuyến trong vòng " + MAX_ADVANCE_DAYS + " ngày tới.");
            }
        }

        BigDecimal min = request.getMinPrice();
        BigDecimal max = request.getMaxPrice();
        if ((min != null && min.signum() < 0) || (max != null && max.signum() < 0)) {
            throw new IllegalArgumentException("Giá vé không được âm.");
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Giá tối thiểu không được lớn hơn giá tối đa.");
        }

        Double rating = request.getMinRating();
        if (rating != null && (rating < 0 || rating > 5)) {
            throw new IllegalArgumentException("Điểm đánh giá tối thiểu phải từ 0 đến 5.");
        }

        parseBusTypes(request.getBusTypes());
        parseTimeSlots(request.getTimeSlots());
        SortOption.fromCode(request.getSortBy());
    }

    /** Kiểm tra rồi chuyển SearchFilterRequest thành SearchCriteria (không còn giá trị null). */
    public SearchCriteria buildCriteria(SearchFilterRequest request) {
        validate(request);

        LocalDateTime now = now();
        LocalDate travelDate = request.getTravelDate();
        LocalDateTime from;
        LocalDateTime to;
        if (travelDate == null) {
            from = now;
            to = now.plusDays(MAX_ADVANCE_DAYS);
        } else {
            from = DateUtils.startOfDay(travelDate);
            if (from.isBefore(now)) {
                from = now; // hôm nay: loại các chuyến đã khởi hành
            }
            to = DateUtils.startOfDay(travelDate.plusDays(1));
        }

        Set<BusType> types = parseBusTypes(request.getBusTypes());
        boolean anyBusType = types.isEmpty() || types.size() == BusType.values().length;
        List<String> busTypeValues = new ArrayList<>();
        for (BusType type : types) {
            busTypeValues.addAll(type.getDatabaseValues());
        }
        if (busTypeValues.isEmpty()) {
            busTypeValues.add(NO_BUS_TYPE);
        }

        Set<TimeSlot> slots = parseTimeSlots(request.getTimeSlots());
        boolean anyTimeSlot = slots.isEmpty() || slots.size() == TimeSlot.values().length;

        return new SearchCriteria(
                normalizeLocation(request.getOrigin()),
                normalizeLocation(request.getDestination()),
                from, to,
                request.getMinPrice() == null ? PRICE_FLOOR : request.getMinPrice(),
                request.getMaxPrice() == null ? PRICE_CEILING : request.getMaxPrice(),
                request.getMinRating() == null ? 0 : request.getMinRating(),
                busTypeValues, anyBusType,
                slots.contains(TimeSlot.MORNING),
                slots.contains(TimeSlot.NOON),
                slots.contains(TimeSlot.EVENING),
                anyTimeSlot);
    }

    private Set<BusType> parseBusTypes(List<String> codes) {
        Set<BusType> result = EnumSet.noneOf(BusType.class);
        if (codes == null) {
            return result;
        }
        for (String code : codes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            result.add(BusType.fromCode(code).orElseThrow(
                    () -> new IllegalArgumentException("Loại xe không hợp lệ: " + code)));
        }
        return result;
    }

    private Set<TimeSlot> parseTimeSlots(List<String> codes) {
        Set<TimeSlot> result = EnumSet.noneOf(TimeSlot.class);
        if (codes == null) {
            return result;
        }
        for (String code : codes) {
            if (code == null || code.isBlank()) {
                continue;
            }
            result.add(TimeSlot.fromCode(code).orElseThrow(
                    () -> new IllegalArgumentException("Khung giờ không hợp lệ: " + code)));
        }
        return result;
    }

    // ------------------------------------------------------------------ sắp xếp + phân trang

    /** Trả về bản sao đã sắp xếp theo tiêu chí (không sửa danh sách gốc). */
    public List<BusTrip> sort(List<BusTrip> trips, SortOption option) {
        List<BusTrip> copy = new ArrayList<>(trips);
        copy.sort(option.getComparator());
        return copy;
    }

    /** Cắt một trang từ danh sách. page bắt đầu từ 1; ngoài khoảng thì được kẹp lại cho hợp lệ. */
    public <T> PageSlice<T> paginate(List<T> items, Integer page, Integer size) {
        int pageSize = (size == null || size < 1) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int total = items.size();
        int totalPages = Math.max(1, (int) Math.ceil(total / (double) pageSize));
        int current = (page == null || page < 1) ? 1 : Math.min(page, totalPages);
        int from = (current - 1) * pageSize;
        int to = Math.min(from + pageSize, total);
        List<T> slice = from >= total ? List.of() : new ArrayList<>(items.subList(from, to));
        return new PageSlice<>(slice, current, pageSize, total, totalPages);
    }

    // ------------------------------------------------------------------ thống kê cho sidebar

    /** Số chuyến theo từng loại xe; luôn có đủ 3 loại (kể cả 0) theo đúng thứ tự enum. */
    public Map<String, Long> countByBusType(List<BusTrip> trips) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (BusType type : BusType.values()) {
            counts.put(type.name(), 0L);
        }
        for (BusTrip trip : trips) {
            BusType.fromCode(trip.getBusType())
                    .ifPresent(type -> counts.computeIfPresent(type.name(), (key, value) -> value + 1));
        }
        return counts;
    }

    /** Số chuyến theo từng khung giờ khởi hành; luôn có đủ 3 khung. */
    public Map<String, Long> countByTimeSlot(List<BusTrip> trips) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (TimeSlot slot : TimeSlot.values()) {
            counts.put(slot.name(), 0L);
        }
        for (BusTrip trip : trips) {
            String key = TimeSlot.of(trip.getDepartureTime()).name();
            counts.computeIfPresent(key, (k, value) -> value + 1);
        }
        return counts;
    }

    /** Khoảng giá nhỏ nhất - lớn nhất, làm tròn ra bước 10.000đ. Danh sách rỗng => 0 - 1.000.000đ. */
    public PriceBounds priceBounds(List<BusTrip> trips) {
        if (trips.isEmpty()) {
            return new PriceBounds(PRICE_FLOOR, new BigDecimal("1000000"));
        }
        BigDecimal min = trips.stream().map(BusTrip::getTicketPrice).min(Comparator.naturalOrder()).orElse(PRICE_FLOOR);
        BigDecimal max = trips.stream().map(BusTrip::getTicketPrice).max(Comparator.naturalOrder()).orElse(PRICE_FLOOR);
        BigDecimal floor = min.divide(PRICE_STEP, 0, RoundingMode.FLOOR).multiply(PRICE_STEP);
        BigDecimal ceil = max.divide(PRICE_STEP, 0, RoundingMode.CEILING).multiply(PRICE_STEP);
        if (ceil.compareTo(floor) <= 0) {
            ceil = floor.add(PRICE_STEP); // tránh thanh trượt có min = max
        }
        return new PriceBounds(floor, ceil);
    }

    /**
     * Nhãn nổi bật cho từng chuyến (khoá = id chuyến): Giá tốt nhất, Khởi hành sớm nhất,
     * Nhà xe đánh giá cao nhất. Mỗi nhãn chỉ gắn khi tiêu chí đó THỰC SỰ có sự khác biệt giữa các chuyến
     * (ví dụ cùng một mức giá thì không có "Giá tốt nhất"), và cần ít nhất 2 chuyến.
     */
    public Map<Long, List<String>> computeBadges(List<BusTrip> trips) {
        Map<Long, List<String>> badges = new HashMap<>();
        if (trips.size() < 2) {
            return badges;
        }

        BusTrip cheapest = Collections.min(trips, SortOption.PRICE_ASC.getComparator());
        BusTrip priciest = Collections.max(trips, Comparator.comparing(BusTrip::getTicketPrice));
        if (cheapest.getTicketPrice().compareTo(priciest.getTicketPrice()) < 0) {
            addBadge(badges, cheapest, "Giá tốt nhất");
        }

        BusTrip earliest = Collections.min(trips, SortOption.DEPARTURE_ASC.getComparator());
        BusTrip latest = Collections.max(trips, Comparator.comparing(BusTrip::getDepartureTime));
        if (earliest.getDepartureTime().isBefore(latest.getDepartureTime())) {
            addBadge(badges, earliest, "Khởi hành sớm nhất");
        }

        BusTrip best = Collections.min(trips, SortOption.RATING_DESC.getComparator());
        BusTrip worst = Collections.max(trips, SortOption.RATING_DESC.getComparator());
        if (SortOption.ratingOf(best) >= 4.0
                && SortOption.ratingOf(best) > SortOption.ratingOf(worst)) {
            addBadge(badges, best, "Nhà xe đánh giá cao nhất");
        }
        return badges;
    }

    private void addBadge(Map<Long, List<String>> badges, BusTrip trip, String label) {
        badges.computeIfAbsent(trip.getId(), key -> new ArrayList<>()).add(label);
    }

    /** Số nhóm bộ lọc sidebar đang bật (hiển thị "Bộ lọc (3)" trên giao diện di động). */
    public int countActiveFilters(SearchCriteria criteria) {
        int count = 0;
        if (criteria.minPrice().compareTo(PRICE_FLOOR) > 0 || criteria.maxPrice().compareTo(PRICE_CEILING) < 0) {
            count++;
        }
        if (!criteria.anyBusType()) {
            count++;
        }
        if (!criteria.anyTimeSlot()) {
            count++;
        }
        if (criteria.minRating() > 0) {
            count++;
        }
        return count;
    }

    /** Nhãn loại xe không trùng, giữ thứ tự enum (dùng cho trang chi tiết tuyến). */
    public List<String> distinctBusTypeLabels(List<BusTrip> trips) {
        Set<String> present = new LinkedHashSet<>();
        for (BusType type : BusType.values()) {
            for (BusTrip trip : trips) {
                if (BusType.fromCode(trip.getBusType()).filter(type::equals).isPresent()) {
                    present.add(type.getLabel());
                    break;
                }
            }
        }
        return new ArrayList<>(present);
    }
}
