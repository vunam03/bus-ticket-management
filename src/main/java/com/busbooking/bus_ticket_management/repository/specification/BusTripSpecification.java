package com.busbooking.bus_ticket_management.repository.specification;

import com.busbooking.bus_ticket_management.dto.BusTripSearchCriteria;
import com.busbooking.bus_ticket_management.model.BusTrip;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class BusTripSpecification {

    public static Specification<BusTrip> getSpecification(BusTripSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(criteria.getDepartureLocation())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("departureLocation")),
                        "%" + criteria.getDepartureLocation().toLowerCase().trim() + "%"
                ));
            }

            if (StringUtils.hasText(criteria.getArrivalLocation())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("arrivalLocation")),
                        "%" + criteria.getArrivalLocation().toLowerCase().trim() + "%"
                ));
            }

            if (criteria.getDepartureTimeFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("departureTime"), criteria.getDepartureTimeFrom()
                ));
            }

            if (criteria.getDepartureTimeTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("departureTime"), criteria.getDepartureTimeTo()
                ));
            }

            if (criteria.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("ticketPrice"), criteria.getMinPrice()
                ));
            }

            if (criteria.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("ticketPrice"), criteria.getMaxPrice()
                ));
            }

            if (StringUtils.hasText(criteria.getBusType())) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("busType")),
                        criteria.getBusType().toLowerCase().trim()
                ));
            }

            if (criteria.getCompanyId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("busCompany").get("id"), criteria.getCompanyId()
                ));
            }

            if (StringUtils.hasText(criteria.getCompanyName())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("busCompany").get("name")),
                        "%" + criteria.getCompanyName().toLowerCase().trim() + "%"
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}