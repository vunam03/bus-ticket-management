package com.busbooking.bus_ticket_management.utils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Tiện ích xử lý ngày giờ cho Module 3 (múi giờ Việt Nam, định dạng tiếng Việt).
 * Lớp tiện ích: chỉ có phương thức static, không cho tạo đối tượng.
 */
public final class DateUtils {

    public static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter SHORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private static final String[] DAY_NAMES = {
            "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ nhật"
    };
    private static final String[] SHORT_DAY_NAMES = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private DateUtils() {
    }

    /** Ngày hiện tại theo giờ Việt Nam. */
    public static LocalDate today() {
        return LocalDate.now(VN_ZONE);
    }

    /** 00:00:00 của ngày đã cho. */
    public static LocalDateTime startOfDay(LocalDate date) {
        return date.atStartOfDay();
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    public static String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DATE_FORMAT);
    }

    public static String formatShortDate(LocalDate date) {
        return date == null ? "" : date.format(SHORT_DATE_FORMAT);
    }

    public static String formatTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(TIME_FORMAT);
    }

    /** Tên thứ đầy đủ: "Thứ Hai", ..., "Chủ nhật". */
    public static String dayOfWeekName(LocalDate date) {
        return DAY_NAMES[date.getDayOfWeek().getValue() - 1];
    }

    /** Tên thứ rút gọn: "T2", ..., "CN". */
    public static String shortDayName(LocalDate date) {
        return SHORT_DAY_NAMES[date.getDayOfWeek().getValue() - 1];
    }

    /** Ví dụ: "Thứ Hai, 09/11/2026". */
    public static String fullDateLabel(LocalDate date) {
        return date == null ? "Mọi ngày" : dayOfWeekName(date) + ", " + formatDate(date);
    }

    /** "Hôm nay" / "Ngày mai" / chuỗi rỗng. */
    public static String relativeDayLabel(LocalDate date, LocalDate today) {
        if (date.equals(today)) {
            return "Hôm nay";
        }
        if (date.equals(today.plusDays(1))) {
            return "Ngày mai";
        }
        return "";
    }

    public static long minutesBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return -1;
        }
        return Duration.between(from, to).toMinutes();
    }

    /** Ví dụ: 390 phút => "6 giờ 30 phút"; 120 => "2 giờ"; 45 => "45 phút"; âm => "—". */
    public static String formatDuration(long totalMinutes) {
        if (totalMinutes < 0) {
            return "—";
        }
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours == 0) {
            return minutes + " phút";
        }
        if (minutes == 0) {
            return hours + " giờ";
        }
        return hours + " giờ " + minutes + " phút";
    }

    public static String formatDuration(LocalDateTime from, LocalDateTime to) {
        return formatDuration(minutesBetween(from, to));
    }

    /** Đọc ngày từ chuỗi "yyyy-MM-dd" hoặc "dd/MM/yyyy". Sai định dạng => Optional.empty(). */
    public static Optional<LocalDate> parseDate(String text) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        String value = text.trim();
        try {
            if (value.contains("/")) {
                return Optional.of(LocalDate.parse(value, DATE_FORMAT));
            }
            return Optional.of(LocalDate.parse(value));
        } catch (DateTimeParseException ex) {
            return Optional.empty();
        }
    }

    /**
     * Chuyển giá trị ngày trả về từ Native Query sang LocalDate.
     * Tuỳ CSDL/Hibernate mà cột DATE có thể là java.sql.Date, Timestamp, LocalDate... nên cần hàm này.
     */
    public static LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof java.util.Date date) {
            return date.toInstant().atZone(VN_ZONE).toLocalDate();
        }
        if (value instanceof CharSequence text) {
            return LocalDate.parse(text.toString().substring(0, 10));
        }
        throw new IllegalArgumentException("Không chuyển được sang LocalDate: " + value);
    }
}
