package com.busbooking.bus_ticket_management.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Optional;

/**
 * Định dạng tiền Việt Nam (VND): dấu chấm phân tách hàng nghìn, hậu tố "đ".
 * Ví dụ: 350000 => "350.000đ". Không phụ thuộc Locale của máy chạy.
 */
public final class CurrencyFormatter {

    public static final String SUFFIX = "đ";

    private CurrencyFormatter() {
    }

    /** DecimalFormat không an toàn đa luồng nên tạo mới mỗi lần gọi. */
    private static DecimalFormat newFormat(String pattern) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.ROOT);
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat(pattern, symbols);
    }

    /** 350000 => "350.000đ". null => "Liên hệ". */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "Liên hệ";
        }
        return newFormat("#,##0").format(amount.setScale(0, RoundingMode.HALF_UP)) + SUFFIX;
    }

    public static String format(double amount) {
        return format(BigDecimal.valueOf(amount));
    }

    /** "250.000đ - 450.000đ"; nếu hai đầu bằng nhau thì chỉ in một giá. */
    public static String formatRange(BigDecimal min, BigDecimal max) {
        if (min == null || max == null) {
            return "Liên hệ";
        }
        if (min.compareTo(max) == 0) {
            return format(min);
        }
        return format(min) + " - " + format(max);
    }

    /** Dạng gọn cho nhãn nhỏ: 350000 => "350K", 1200000 => "1,2tr". */
    public static String compact(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        long value = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        if (value >= 1_000_000) {
            String millions = newFormat("0.#").format(value / 1_000_000.0);
            return millions + "tr";
        }
        return (value / 1000) + "K";
    }

    /** Đọc lại số tiền từ chuỗi như "350.000đ" hoặc "350,000 VND". Không có chữ số => Optional.empty(). */
    public static Optional<BigDecimal> parse(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(digits));
    }

    /** Chuyển giá trị số bất kỳ (Native Query trả về BigDecimal/Double/Long...) sang BigDecimal. */
    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }
}
