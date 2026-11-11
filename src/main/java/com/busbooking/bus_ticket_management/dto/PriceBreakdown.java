package com.busbooking.bus_ticket_management.dto;

import java.math.BigDecimal;

// Chi tiết giá của một vé sau khi tính phụ phí và ưu đãi.
public record PriceBreakdown(
        BigDecimal base,
        BigDecimal surcharge,
        BigDecimal discount,
        BigDecimal total
) {
}