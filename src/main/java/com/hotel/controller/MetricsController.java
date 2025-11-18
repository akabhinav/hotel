package com.hotel.controller;

import com.hotel.service.CachedInventoryService;
import com.hotel.service.InventoryCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller for cache management and metrics endpoints.
 * Used for monitoring and managing the caching layer.
 */
@RestController
@RequestMapping("/v1/admin")
@RequiredArgsConstructor
public class MetricsController {

    private final InventoryCacheService cacheService;
    private final CachedInventoryService cachedInventoryService;

    /**
     * Check cache health and status
     */
    @GetMapping("/cache/status")
    public ResponseEntity<Map<String, Object>> getCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("cacheAvailable", cacheService.isCacheAvailable());
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    /**
     * Warm up cache for a specific hotel and room type
     */
    @PostMapping("/cache/warmup")
    public ResponseEntity<Map<String, Object>> warmUpCache(
            @RequestParam Long hotelId,
            @RequestParam Long roomTypeId,
            @RequestParam(defaultValue = "30") int daysAhead) {

        cachedInventoryService.warmUpCache(hotelId, roomTypeId, daysAhead);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("hotelId", hotelId);
        result.put("roomTypeId", roomTypeId);
        result.put("daysWarmedUp", daysAhead);
        result.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(result);
    }

    /**
     * Invalidate cache for a date range
     */
    @DeleteMapping("/cache/invalidate")
    public ResponseEntity<Map<String, Object>> invalidateCache(
            @RequestParam Long hotelId,
            @RequestParam Long roomTypeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        cachedInventoryService.invalidateCache(hotelId, roomTypeId, startDate, endDate);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("hotelId", hotelId);
        result.put("roomTypeId", roomTypeId);
        result.put("startDate", startDate);
        result.put("endDate", endDate);

        return ResponseEntity.ok(result);
    }

    /**
     * Get scaling information
     */
    @GetMapping("/scaling/info")
    public ResponseEntity<Map<String, Object>> getScalingInfo() {
        Map<String, Object> info = new HashMap<>();

        // Cache configuration
        Map<String, Object> cacheInfo = new HashMap<>();
        cacheInfo.put("type", "Redis");
        cacheInfo.put("available", cacheService.isCacheAvailable());
        info.put("cache", cacheInfo);

        // Database configuration
        Map<String, Object> dbInfo = new HashMap<>();
        dbInfo.put("poolName", "HotelDB-Pool");
        dbInfo.put("maxPoolSize", 20);
        dbInfo.put("minIdle", 5);
        info.put("database", dbInfo);

        // Sharding configuration
        Map<String, Object> shardInfo = new HashMap<>();
        shardInfo.put("enabled", false);
        shardInfo.put("shardCount", 4);
        shardInfo.put("shardKey", "hotel_id");
        info.put("sharding", shardInfo);

        return ResponseEntity.ok(info);
    }
}
