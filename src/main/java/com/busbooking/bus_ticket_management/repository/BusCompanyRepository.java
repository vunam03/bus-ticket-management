package com.busbooking.bus_ticket_management.repository;

import com.busbooking.bus_ticket_management.model.BusCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusCompanyRepository extends JpaRepository<BusCompany, Long> {
}