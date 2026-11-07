package com.busbooking.bus_ticket_management.service;

import com.busbooking.bus_ticket_management.model.BusCompany;
import com.busbooking.bus_ticket_management.repository.BusCompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BusCompanyService {

    @Autowired
    private BusCompanyRepository busCompanyRepository;

    public List<BusCompany> getAllCompanies() {
        return busCompanyRepository.findAll();
    }

    public BusCompany getCompanyById(Long id) {
        return busCompanyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy nhà xe với ID: " + id));
    }

    @Transactional
    public void saveCompany(BusCompany company) {
        busCompanyRepository.save(company);
    }

    @Transactional
    public void deleteCompany(Long id) {
        if (!busCompanyRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy nhà xe để xóa!");
        }
        busCompanyRepository.deleteById(id);
    }
}