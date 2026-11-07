package com.busbooking.bus_ticket_management.cache;

import com.busbooking.bus_ticket_management.dto.BusTripResponse;
import com.busbooking.bus_ticket_management.model.BusCompany;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BusModuleCacheManager {

    private final Map<Long, BusCompany> companyCache = new ConcurrentHashMap<>();
    private final Map<String, List<BusTripResponse>> tripSearchCache = new ConcurrentHashMap<>();
    private long lastCacheClearTime = System.currentTimeMillis();
    private static final long CACHE_TTL_MS = 300000; // 5 phút (Time To Live)

    public void putCompany(Long id, BusCompany company) {
        checkAndEvictExpiredCache();
        companyCache.put(id, company);
    }

    public BusCompany getCompany(Long id) {
        checkAndEvictExpiredCache();
        return companyCache.get(id);
    }

    public void putTripSearchResults(String searchKey, List<BusTripResponse> results) {
        checkAndEvictExpiredCache();
        tripSearchCache.put(searchKey, results);
    }

    public List<BusTripResponse> getTripSearchResults(String searchKey) {
        checkAndEvictExpiredCache();
        return tripSearchCache.get(searchKey);
    }

    public void clearAllCache() {
        companyCache.clear();
        tripSearchCache.clear();
        lastCacheClearTime = System.currentTimeMillis();
    }

    private void checkAndEvictExpiredCache() {
        if (System.currentTimeMillis() - lastCacheClearTime > CACHE_TTL_MS) {
            clearAllCache();
        }
    }
}