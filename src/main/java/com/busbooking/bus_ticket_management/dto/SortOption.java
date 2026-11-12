package com.busbooking.bus_ticket_management.dto;

import com.busbooking.bus_ticket_management.model.BusTrip;

import java.util.Comparator;

/**
 * Các tiêu chí sắp xếp kết quả. Mỗi tiêu chí đóng gói sẵn một Comparator
 * (mẫu thiết kế Strategy) nên FilterService chỉ cần gọi sort(option) mà không cần if/else.
 */
public enum SortOption {

    /** Giá từ thấp đến cao; bằng giá thì chuyến đi sớm hơn đứng trước. */
    PRICE_ASC("Giá thấp nhất",
            Comparator.comparing(BusTrip::getTicketPrice)
                    .thenComparing(BusTrip::getDepartureTime)),

    /** Giờ đi sớm nhất; cùng giờ thì giá rẻ hơn đứng trước. */
    DEPARTURE_ASC("Giờ đi sớm nhất",
            Comparator.comparing(BusTrip::getDepartureTime)
                    .thenComparing(BusTrip::getTicketPrice)),

    /** Đánh giá của nhà xe cao nhất; bằng điểm thì giá rẻ hơn, rồi giờ đi sớm hơn đứng trước. */
    RATING_DESC("Đánh giá cao nhất",
            Comparator.<BusTrip>comparingDouble(SortOption::ratingOf).reversed()
                    .thenComparing(BusTrip::getTicketPrice)
                    .thenComparing(BusTrip::getDepartureTime));

    public static final SortOption DEFAULT = PRICE_ASC;

    /** Điểm đánh giá của nhà xe; nhà xe chưa có điểm (null) được tính là 0 để không gây NullPointerException. */
    public static double ratingOf(BusTrip trip) {
        if (trip.getBusCompany() == null || trip.getBusCompany().getRating() == null) {
            return 0;
        }
        return trip.getBusCompany().getRating();
    }

    private final String label;
    private final Comparator<BusTrip> comparator;

    SortOption(String label, Comparator<BusTrip> comparator) {
        this.label = label;
        this.comparator = comparator;
    }

    public String getLabel() {
        return label;
    }

    public Comparator<BusTrip> getComparator() {
        return comparator;
    }

    /**
     * Chuyển mã (chuỗi từ URL) thành SortOption.
     * Rỗng/null => mặc định; mã lạ => ném IllegalArgumentException để Controller trả lỗi 400.
     */
    public static SortOption fromCode(String code) {
        if (code == null || code.isBlank()) {
            return DEFAULT;
        }
        String normalized = code.trim().toUpperCase();
        for (SortOption option : values()) {
            if (option.name().equals(normalized)) {
                return option;
            }
        }
        throw new IllegalArgumentException("Tiêu chí sắp xếp không hợp lệ: " + code);
    }
}
