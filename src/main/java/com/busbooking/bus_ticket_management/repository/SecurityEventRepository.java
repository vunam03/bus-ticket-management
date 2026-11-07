package com.busbooking.bus_ticket_management.repository;


import com.busbooking.bus_ticket_management.model.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {
    Page<SecurityEvent> findByUserIdOrderByOccurredAtDescIdDesc(Long userId, Pageable page);
    Page<SecurityEvent> findByUserIdAndTypeOrderByOccurredAtDescIdDesc(
            Long userId, SecurityEventType type, Pageable page);
    long deleteByOccurredAtBefore(Instant cutoff);
}
