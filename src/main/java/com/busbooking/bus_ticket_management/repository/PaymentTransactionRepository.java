package com.busbooking.bus_ticket_management.repository;

import com.busbooking.bus_ticket_management.model.PaymentTransaction;
import com.busbooking.bus_ticket_management.model.PaymentTransactionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentTransactionRepository
        extends JpaRepository<PaymentTransaction, Long> {

    // Tra cứu bằng mã giao dịch duy nhất.
    Optional<PaymentTransaction> findByTransactionCode(
            String transactionCode
    );

    // Chỉ tìm giao dịch thuộc vé của đúng khách hàng.
    Optional<PaymentTransaction> findByTransactionCodeAndTicket_User_Id(
            String transactionCode,
            Long userId
    );

    // Liệt kê các lần thanh toán của một vé, mới nhất trước.
    Page<PaymentTransaction> findByTicket_IdOrderByCreatedAtDescIdDesc(
            Long ticketId,
            Pageable pageable
    );

    // Lấy giao dịch gần nhất của vé.
    Optional<PaymentTransaction> findFirstByTicket_IdOrderByCreatedAtDescIdDesc(
            Long ticketId
    );

    // Kiểm tra vé đã có giao dịch với trạng thái chỉ định hay chưa.
    boolean existsByTicket_IdAndStatus(
            Long ticketId,
            PaymentTransactionStatus status
    );

    // Lịch sử giao dịch của khách hàng, có phân trang.
    Page<PaymentTransaction> findByTicket_User_IdOrderByCreatedAtDescIdDesc(
            Long userId,
            Pageable pageable
    );

    /*
     * Khóa giao dịch khi ghi nhận kết quả.
     * Service gọi phương thức này phải nằm trong @Transactional.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from PaymentTransaction p
            where p.transactionCode = :transactionCode
            """)
    Optional<PaymentTransaction> findByTransactionCodeForUpdate(
            @Param("transactionCode") String transactionCode
    );
    java.util.List<PaymentTransaction> findByTicket_IdOrderByCreatedAtDesc(Long ticketId);
}
