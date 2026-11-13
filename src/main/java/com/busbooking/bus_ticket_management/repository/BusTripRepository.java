package com.busbooking.bus_ticket_management.repository;

import com.busbooking.bus_ticket_management.model.BusTrip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import com.busbooking.bus_ticket_management.model.Seat;

@Repository
public interface BusTripRepository extends JpaRepository<BusTrip, Long> {

    @Query("SELECT t FROM BusTrip t WHERE LOWER(t.departureLocation) LIKE LOWER(CONCAT('%', :departure, '%')) " +
            "AND LOWER(t.arrivalLocation) LIKE LOWER(CONCAT('%', :arrival, '%'))")
    List<BusTrip> searchTrips(@Param("departure") String departure, @Param("arrival") String arrival);

    List<BusTrip> findByBusCompanyId(Long companyId);

    // =====================================================================
    // PHẦN CỦA MODULE 3 (tìm kiếm và lọc chuyến xe cho khách) - Đỗ Hữu Vũ
    // Phần phía trên là của Module 1, giữ nguyên.
    //
    // - JPQL  : thao tác trên entity (tên field Java như t.departureTime).
    // - Native: SQL thuần (tên bảng/cột như bus_trips.departure_time), dùng cho thống kê GROUP BY.
    //
    // Quy ước tham số "tuỳ chọn": KHÔNG truyền null xuống CSDL. FilterService luôn chuẩn hoá
    // thành giá trị mặc định: chuỗi rỗng = bỏ qua, cờ boolean = bật/tắt điều kiện,
    // khoảng giá = cận rộng nhất.
    // =====================================================================

    /**
     * TRUY VẤN CHÍNH (JPQL): tìm chuyến theo điểm đi, điểm đến (khớp đúng, không phân biệt hoa/thường),
     * khoảng thời gian khởi hành, khoảng giá, đánh giá nhà xe tối thiểu, loại xe và khung giờ.
     * <p>
     * JOIN FETCH nạp luôn nhà xe (quan hệ LAZY) trong một câu SQL, tránh LazyInitializationException
     * (spring.jpa.open-in-view=false) và tránh truy vấn N+1.
     * COALESCE: nhà xe chưa có điểm đánh giá (rating = NULL) được tính là 0, không bị loại khỏi kết quả.
     * <p>
     * Khung giờ (khớp enum TimeSlot): Sáng 5-11h59, Trưa 12-17h59, Tối 18h-4h59 (qua nửa đêm).
     * busTypes: các cách viết loại xe viết thường (xem BusType#getDatabaseValues).
     */
    @Query("""
            SELECT t FROM BusTrip t JOIN FETCH t.busCompany c
            WHERE (:origin = '' OR LOWER(t.departureLocation) = LOWER(:origin))
              AND (:destination = '' OR LOWER(t.arrivalLocation) = LOWER(:destination))
              AND t.departureTime >= :fromTime
              AND t.departureTime < :toTime
              AND t.ticketPrice BETWEEN :minPrice AND :maxPrice
              AND COALESCE(c.rating, 0.0) >= :minRating
              AND (:anyBusType = true OR LOWER(t.busType) IN (:busTypes))
              AND (:anyTimeSlot = true
                   OR (:morning = true
                       AND EXTRACT(HOUR FROM t.departureTime) >= 5
                       AND EXTRACT(HOUR FROM t.departureTime) < 12)
                   OR (:noon = true
                       AND EXTRACT(HOUR FROM t.departureTime) >= 12
                       AND EXTRACT(HOUR FROM t.departureTime) < 18)
                   OR (:evening = true
                       AND (EXTRACT(HOUR FROM t.departureTime) >= 18
                            OR EXTRACT(HOUR FROM t.departureTime) < 5)))
            ORDER BY t.departureTime ASC
            """)
    List<BusTrip> searchByFilters(@Param("origin") String origin,
                                  @Param("destination") String destination,
                                  @Param("fromTime") LocalDateTime fromTime,
                                  @Param("toTime") LocalDateTime toTime,
                                  @Param("minPrice") BigDecimal minPrice,
                                  @Param("maxPrice") BigDecimal maxPrice,
                                  @Param("minRating") double minRating,
                                  @Param("busTypes") List<String> busTypes,
                                  @Param("anyBusType") boolean anyBusType,
                                  @Param("morning") boolean morning,
                                  @Param("noon") boolean noon,
                                  @Param("evening") boolean evening,
                                  @Param("anyTimeSlot") boolean anyTimeSlot);

    /** Các chuyến sắp chạy của một tuyến (dùng cho trang chi tiết tuyến). */
    @Query("""
            SELECT t FROM BusTrip t JOIN FETCH t.busCompany
            WHERE LOWER(t.departureLocation) = LOWER(:origin)
              AND LOWER(t.arrivalLocation) = LOWER(:destination)
              AND t.departureTime >= :fromTime
            ORDER BY t.departureTime ASC
            """)
    List<BusTrip> findUpcomingByRoute(@Param("origin") String origin,
                                      @Param("destination") String destination,
                                      @Param("fromTime") LocalDateTime fromTime);

    /** Danh sách điểm đi (không trùng) để gợi ý trong ô tìm kiếm. */
    @Query("SELECT DISTINCT t.departureLocation FROM BusTrip t ORDER BY t.departureLocation")
    List<String> findDistinctOrigins();

    /** Danh sách điểm đến (không trùng) để gợi ý trong ô tìm kiếm. */
    @Query("SELECT DISTINCT t.arrivalLocation FROM BusTrip t ORDER BY t.arrivalLocation")
    List<String> findDistinctDestinations();

    /** Điểm đến khả dụng khi đã chọn điểm đi (thu hẹp gợi ý). */
    @Query("""
            SELECT DISTINCT t.arrivalLocation FROM BusTrip t
            WHERE LOWER(t.departureLocation) = LOWER(:origin)
            ORDER BY t.arrivalLocation
            """)
    List<String> findDestinationsByOrigin(@Param("origin") String origin);

    /**
     * Số ghế KHÔNG ở trạng thái "status" của từng chuyến (bảng bus_seats của Module 4).
     * Gọi với status = AVAILABLE để biết mỗi chuyến có bao nhiêu ghế đang giữ/đã đặt.
     * Chuyến chưa có dòng ghế nào (Module 4 tạo sơ đồ ghế khi có người mở) sẽ không xuất hiện trong kết quả.
     * Mỗi hàng: [0] id chuyến, [1] số ghế.
     */
    @Query("""
            SELECT s.busTrip.id, COUNT(s) FROM Seat s
            WHERE s.busTrip.id IN (:tripIds) AND s.seatStatus <> :status
            GROUP BY s.busTrip.id
            """)
    List<Object[]> countSeatsNotInStatus(@Param("tripIds") Collection<Long> tripIds,
                                         @Param("status") Seat.SeatStatus status);

    /**
     * NATIVE SQL #1 - Giá thấp nhất mỗi ngày của một tuyến (dải chọn ngày).
     * Mỗi hàng: [0] ngày (DATE), [1] giá thấp nhất, [2] số chuyến.
     */
    @Query(value = """
            SELECT CAST(t.departure_time AS DATE) AS trip_date,
                   MIN(t.ticket_price)            AS min_price,
                   COUNT(*)                       AS trip_count
            FROM bus_trips t
            WHERE LOWER(t.departure_location) = LOWER(:origin)
              AND LOWER(t.arrival_location) = LOWER(:destination)
              AND t.departure_time >= :fromTime
              AND t.departure_time < :toTime
            GROUP BY CAST(t.departure_time AS DATE)
            ORDER BY trip_date
            """, nativeQuery = true)
    List<Object[]> findDailyMinPrices(@Param("origin") String origin,
                                      @Param("destination") String destination,
                                      @Param("fromTime") LocalDateTime fromTime,
                                      @Param("toTime") LocalDateTime toTime);

    /**
     * NATIVE SQL #2 - Thống kê theo tuyến để gợi ý tuyến phổ biến.
     * Mỗi hàng: [0] điểm đi, [1] điểm đến, [2] số chuyến, [3] giá thấp nhất, [4] giá cao nhất,
     *           [5] đánh giá trung bình của nhà xe, [6] tổng số ghế.
     * origin = '' nghĩa là lấy mọi tuyến.
     */
    @Query(value = """
            SELECT t.departure_location, t.arrival_location,
                   COUNT(*)            AS trip_count,
                   MIN(t.ticket_price) AS min_price,
                   MAX(t.ticket_price) AS max_price,
                   AVG(c.rating)       AS avg_rating,
                   SUM(t.total_seats)  AS total_seats
            FROM bus_trips t
            JOIN bus_companies c ON c.id = t.company_id
            WHERE t.departure_time >= :fromTime
              AND (:origin = '' OR LOWER(t.departure_location) = LOWER(:origin))
            GROUP BY t.departure_location, t.arrival_location
            ORDER BY trip_count DESC, avg_rating DESC
            """, nativeQuery = true)
    List<Object[]> findRouteStatistics(@Param("origin") String origin,
                                       @Param("fromTime") LocalDateTime fromTime);
}