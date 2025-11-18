package com.hotel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based caching service for inventory data.
 * Provides high-performance read access to room availability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryCacheService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String INVENTORY_KEY_PREFIX = "inventory:";
    private static final String RATE_KEY_PREFIX = "rate:";
    private static final String HOTEL_KEY_PREFIX = "hotel:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    /**
     * Get available room count from cache
     */
    public Integer getAvailableRooms(Long hotelId, Long roomTypeId, LocalDate date) {
        String key = buildInventoryKey(hotelId, roomTypeId, date);
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value != null) {
                log.debug("Cache HIT for inventory: {}", key);
                return (Integer) value;
            }
            log.debug("Cache MISS for inventory: {}", key);
            return null;
        } catch (Exception e) {
            log.warn("Failed to get from cache: {}", key, e);
            return null;
        }
    }

    /**
     * Update available room count in cache
     */
    public void updateAvailableRooms(Long hotelId, Long roomTypeId, LocalDate date, int available) {
        String key = buildInventoryKey(hotelId, roomTypeId, date);
        try {
            redisTemplate.opsForValue().set(key, available, DEFAULT_TTL);
            log.debug("Cache updated for inventory: {} = {}", key, available);
        } catch (Exception e) {
            log.warn("Failed to update cache: {}", key, e);
        }
    }

    /**
     * Decrement available rooms (atomic operation for reservations)
     */
    public Long decrementAvailableRooms(Long hotelId, Long roomTypeId, LocalDate date, int count) {
        String key = buildInventoryKey(hotelId, roomTypeId, date);
        try {
            Long result = redisTemplate.opsForValue().decrement(key, count);
            log.debug("Cache decremented for inventory: {} by {}, new value: {}", key, count, result);
            return result;
        } catch (Exception e) {
            log.warn("Failed to decrement cache: {}", key, e);
            return null;
        }
    }

    /**
     * Increment available rooms (for cancellations)
     */
    public Long incrementAvailableRooms(Long hotelId, Long roomTypeId, LocalDate date, int count) {
        String key = buildInventoryKey(hotelId, roomTypeId, date);
        try {
            Long result = redisTemplate.opsForValue().increment(key, count);
            log.debug("Cache incremented for inventory: {} by {}, new value: {}", key, count, result);
            return result;
        } catch (Exception e) {
            log.warn("Failed to increment cache: {}", key, e);
            return null;
        }
    }

    /**
     * Cache room rate
     */
    public void cacheRate(Long hotelId, Long roomTypeId, LocalDate date, Double rate) {
        String key = buildRateKey(hotelId, roomTypeId, date);
        try {
            redisTemplate.opsForValue().set(key, rate, DEFAULT_TTL);
            log.debug("Cached rate: {} = {}", key, rate);
        } catch (Exception e) {
            log.warn("Failed to cache rate: {}", key, e);
        }
    }

    /**
     * Get cached room rate
     */
    public Double getCachedRate(Long hotelId, Long roomTypeId, LocalDate date) {
        String key = buildRateKey(hotelId, roomTypeId, date);
        try {
            Object value = redisTemplate.opsForValue().get(key);
            return value != null ? ((Number) value).doubleValue() : null;
        } catch (Exception e) {
            log.warn("Failed to get cached rate: {}", key, e);
            return null;
        }
    }

    /**
     * Invalidate inventory cache for a date range
     */
    public void invalidateInventory(Long hotelId, Long roomTypeId, LocalDate startDate, LocalDate endDate) {
        try {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                String key = buildInventoryKey(hotelId, roomTypeId, date);
                redisTemplate.delete(key);
            }
            log.debug("Invalidated inventory cache for hotel {} room type {} from {} to {}",
                    hotelId, roomTypeId, startDate, endDate);
        } catch (Exception e) {
            log.warn("Failed to invalidate inventory cache", e);
        }
    }

    /**
     * Warm up cache with inventory data
     */
    public void warmUpInventoryCache(Long hotelId, Long roomTypeId, LocalDate startDate,
                                     int days, int[] availableRooms) {
        try {
            for (int i = 0; i < days && i < availableRooms.length; i++) {
                LocalDate date = startDate.plusDays(i);
                String key = buildInventoryKey(hotelId, roomTypeId, date);
                redisTemplate.opsForValue().set(key, availableRooms[i], DEFAULT_TTL);
            }
            log.info("Warmed up inventory cache for hotel {} room type {} with {} days",
                    hotelId, roomTypeId, days);
        } catch (Exception e) {
            log.warn("Failed to warm up inventory cache", e);
        }
    }

    /**
     * Check if cache is available (for fallback logic)
     */
    public boolean isCacheAvailable() {
        try {
            redisTemplate.opsForValue().get("health-check");
            return true;
        } catch (Exception e) {
            log.warn("Redis cache is not available", e);
            return false;
        }
    }

    /**
     * Set TTL for a key
     */
    public void setTTL(String key, long timeout, TimeUnit unit) {
        try {
            redisTemplate.expire(key, timeout, unit);
        } catch (Exception e) {
            log.warn("Failed to set TTL for key: {}", key, e);
        }
    }

    private String buildInventoryKey(Long hotelId, Long roomTypeId, LocalDate date) {
        return INVENTORY_KEY_PREFIX + hotelId + ":" + roomTypeId + ":" + date;
    }

    private String buildRateKey(Long hotelId, Long roomTypeId, LocalDate date) {
        return RATE_KEY_PREFIX + hotelId + ":" + roomTypeId + ":" + date;
    }

    private String buildHotelKey(Long hotelId) {
        return HOTEL_KEY_PREFIX + hotelId;
    }
}
