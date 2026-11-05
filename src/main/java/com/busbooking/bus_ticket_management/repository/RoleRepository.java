package com.busbooking.bus_ticket_management.repository;


import com.busbooking.bus_ticket_management.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
}
