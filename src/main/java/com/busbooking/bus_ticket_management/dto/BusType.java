package com.busbooking.bus_ticket_management.dto;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Loại xe mà bộ lọc hỗ trợ. Cột bus_type của BusTrip là chuỗi tự do (nhập từ form quản trị),
 * nên việc nhận diện KHÔNG so khớp tuyệt đối mà so theo "khóa không dấu":
 * "Giường nằm", "giuong nam", "GIUONG_NAM" đều nhận ra là GIUONG_NAM.
 * <p>
 * Khuyến nghị cho form thêm chuyến (Module 1): dùng ô chọn với đúng 3 giá trị
 * "Giường nằm", "Limousine", "Ghế ngồi".
 */
public enum BusType {

    GIUONG_NAM("Giường nằm"),
    LIMOUSINE("Limousine"),
    GHE_NGOI("Ghế ngồi");

    private final String label;

    BusType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Khóa so sánh: bỏ dấu, chữ thường, mọi ký tự không phải chữ-số gộp thành "_".
     * "Giường nằm" -> "giuong_nam"; "GIUONG_NAM" -> "giuong_nam"; "Ghế ngồi" -> "ghe_ngoi".
     */
    public static String normalizeKey(String text) {
        if (text == null) {
            return "";
        }
        String s = text.trim().replace('đ', 'd').replace('Đ', 'D');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    /** Nhận diện từ mã tham số URL hoặc từ giá trị lưu trong CSDL. Không nhận ra => Optional.empty(). */
    public static Optional<BusType> fromCode(String value) {
        String key = normalizeKey(value);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        for (BusType type : values()) {
            if (normalizeKey(type.label).equals(key)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /** Nhãn hiển thị; giá trị lạ trong CSDL được giữ nguyên để không làm vỡ giao diện. */
    public static String labelOf(String databaseValue) {
        if (databaseValue == null || databaseValue.isBlank()) {
            return "Khác";
        }
        return fromCode(databaseValue).map(BusType::getLabel).orElse(databaseValue);
    }

    /**
     * Các cách viết (chữ thường) có thể có trong cột bus_type, dùng cho mệnh đề
     * LOWER(t.busType) IN (...) của câu truy vấn.
     */
    public List<String> getDatabaseValues() {
        String plain = normalizeKey(label).replace('_', ' ');
        Set<String> values = new LinkedHashSet<>();
        values.add(label.toLowerCase(Locale.ROOT));
        values.add(plain);
        values.add(plain.replace(' ', '_'));
        values.add(plain.replace(' ', '-'));
        values.add(plain.replace(" ", ""));
        return List.copyOf(values);
    }
}
