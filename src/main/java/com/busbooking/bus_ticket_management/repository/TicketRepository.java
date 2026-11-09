package com.busbooking.bus_ticket_management.repository;

import com.busbooking.bus_ticket_management.model.Ticket;
import com.busbooking.bus_ticket_management.model.TicketStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Tra cứu vé bằng mã vé duy nhất.
    Optional<Ticket> findByTicketCode(String ticketCode);

    // Tra cứu kèm chủ sở hữu để tránh xem vé của người khác.
    Optional<Ticket> findByTicketCodeAndUser_Id(
            String ticketCode,
            Long userId
    );

    Optional<Ticket> findByIdAndUser_Id(Long id, Long userId);

    // Lịch sử vé của khách hàng, mới nhất trước, có phân trang.
    Page<Ticket> findByUser_IdOrderByCreatedAtDescIdDesc(
            Long userId,
            Pageable pageable
    );

    // Lọc lịch sử theo trạng thái vé.
    Page<Ticket> findByUser_IdAndTicketStatusOrderByCreatedAtDescIdDesc(
            Long userId,
            TicketStatus ticketStatus,
            Pageable pageable
    );

    // Kiểm tra ghế có vé thuộc các trạng thái cần xét hay không.
    boolean existsBySeat_IdAndTicketStatusIn(
            Long seatId,
            Collection<TicketStatus> statuses
    );

    // Phục vụ tra cứu vé theo chuyến xe.
    Page<Ticket> findByBusTrip_IdOrderByCreatedAtDescIdDesc(
            Long tripId,
            Pageable pageable
    );

    /*
     * Khóa bản ghi vé khi cập nhật thanh toán hoặc hủy vé.
     * Service gọi phương thức này phải nằm trong @Transactional.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Ticket t where t.id = :ticketId")
    Optional<Ticket> findByIdForUpdate(
            @Param("ticketId") Long ticketId
    );
}