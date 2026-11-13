package com.busbooking.bus_ticket_management.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Toàn bộ dữ liệu một lần tìm kiếm: danh sách chuyến (đã sắp xếp + phân trang),
 * thông tin phân trang, khoảng giá cho thanh trượt, số lượng theo từng bộ lọc và dải giá theo ngày.
 * Dùng cho cả trang HTML (/search) lẫn JSON (/search/filter).
 */
public class SearchResponseDto {

    private List<TripSearchResultDto> trips = new ArrayList<>();
    private int page = 1;
    private int size = 10;
    private int totalItems;
    private int totalPages = 1;
    /** Số chuyến thoả điều kiện tuyến + ngày, TRƯỚC khi áp bộ lọc sidebar. */
    private int totalBeforeFilters;
    private BigDecimal priceMin = BigDecimal.ZERO;
    private BigDecimal priceMax = new BigDecimal("1000000");
    private List<Facet> busTypeFacets = new ArrayList<>();
    private List<Facet> timeSlotFacets = new ArrayList<>();
    private List<DailyPrice> dailyPrices = new ArrayList<>();
    private String sortBy = SortOption.DEFAULT.name();
    private String sortLabel = SortOption.DEFAULT.getLabel();
    private String title = "Tất cả chuyến xe";
    private String dateLabel = "Mọi ngày";
    private int activeFilterCount;

    /** Kết quả rỗng, dùng khi yêu cầu không hợp lệ để trang vẫn hiển thị được. */
    public static SearchResponseDto empty() {
        SearchResponseDto dto = new SearchResponseDto();
        for (BusType type : BusType.values()) {
            dto.busTypeFacets.add(new Facet(type.name(), type.getLabel(), "", 0));
        }
        for (TimeSlot slot : TimeSlot.values()) {
            dto.timeSlotFacets.add(new Facet(slot.name(), slot.getLabel(), slot.getRange(), 0));
        }
        return dto;
    }

    /** Một lựa chọn trong sidebar (loại xe hoặc khung giờ) kèm số chuyến tương ứng. */
    public static class Facet {
        private final String code;
        private final String label;
        private final String hint;
        private final long count;

        public Facet(String code, String label, String hint, long count) {
            this.code = code;
            this.label = label;
            this.hint = hint;
            this.count = count;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public String getHint() {
            return hint;
        }

        public long getCount() {
            return count;
        }
    }

    /** Giá thấp nhất của một ngày, dùng cho dải chọn ngày phía trên danh sách. */
    public static class DailyPrice {
        private final String date;
        private final String label;
        private final String relative;
        private final BigDecimal minPrice;
        private final String priceText;
        private final long tripCount;
        private final boolean selected;

        public DailyPrice(String date, String label, String relative, BigDecimal minPrice,
                          String priceText, long tripCount, boolean selected) {
            this.date = date;
            this.label = label;
            this.relative = relative;
            this.minPrice = minPrice;
            this.priceText = priceText;
            this.tripCount = tripCount;
            this.selected = selected;
        }

        /** yyyy-MM-dd */
        public String getDate() {
            return date;
        }

        /** Ví dụ "T2 09/11" */
        public String getLabel() {
            return label;
        }

        /** "Hôm nay", "Ngày mai" hoặc rỗng */
        public String getRelative() {
            return relative;
        }

        public BigDecimal getMinPrice() {
            return minPrice;
        }

        public String getPriceText() {
            return priceText;
        }

        public long getTripCount() {
            return tripCount;
        }

        public boolean isSelected() {
            return selected;
        }
    }

    public List<TripSearchResultDto> getTrips() {
        return trips;
    }

    public void setTrips(List<TripSearchResultDto> trips) {
        this.trips = trips;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getTotalBeforeFilters() {
        return totalBeforeFilters;
    }

    public void setTotalBeforeFilters(int totalBeforeFilters) {
        this.totalBeforeFilters = totalBeforeFilters;
    }

    public BigDecimal getPriceMin() {
        return priceMin;
    }

    public void setPriceMin(BigDecimal priceMin) {
        this.priceMin = priceMin;
    }

    public BigDecimal getPriceMax() {
        return priceMax;
    }

    public void setPriceMax(BigDecimal priceMax) {
        this.priceMax = priceMax;
    }

    public List<Facet> getBusTypeFacets() {
        return busTypeFacets;
    }

    public void setBusTypeFacets(List<Facet> busTypeFacets) {
        this.busTypeFacets = busTypeFacets;
    }

    public List<Facet> getTimeSlotFacets() {
        return timeSlotFacets;
    }

    public void setTimeSlotFacets(List<Facet> timeSlotFacets) {
        this.timeSlotFacets = timeSlotFacets;
    }

    public List<DailyPrice> getDailyPrices() {
        return dailyPrices;
    }

    public void setDailyPrices(List<DailyPrice> dailyPrices) {
        this.dailyPrices = dailyPrices;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getSortLabel() {
        return sortLabel;
    }

    public void setSortLabel(String sortLabel) {
        this.sortLabel = sortLabel;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDateLabel() {
        return dateLabel;
    }

    public void setDateLabel(String dateLabel) {
        this.dateLabel = dateLabel;
    }

    public int getActiveFilterCount() {
        return activeFilterCount;
    }

    public void setActiveFilterCount(int activeFilterCount) {
        this.activeFilterCount = activeFilterCount;
    }
}
