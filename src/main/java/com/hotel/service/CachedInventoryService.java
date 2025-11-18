package com.hotel.service;

import com.hotel.model.RoomTypeInventory;
import com.hotel.repository.RoomTypeInventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Cache-aware inventory service that provides a caching layer
 * on top of database operations for improved performance.
 *
 * This service implements the cache-aside pattern:
 * 1. Check cache first
 * 2. If miss, query database and populate cache
 * 3. Return result
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CachedInventoryService {

    private final RoomTypeInventoryRepository inventoryRepository;
    private final InventoryCacheService cacheService;

    /**
     * Check availability with cache support
     */
    @Transactional(readOnly = true)
    public boolean checkAvailabilityWithCache(Long hotelId, Long roomTypeId,
                                              LocalDate startDate, LocalDate endDate,
                                              int roomCount) {
        // Try to get from cache first
        boolean allCached = true;
        for (LocalDate date = startDate; date.isBefore(endDate); date = date.plusDays(1)) {
            Integer cached = cacheService.getAvailableRooms(hotelId, roomTypeId, date);
            if (cached == null) {
                allCached = false;
                break;
            }
            // Check overbooking support (10%)
            int maxAllowed = (int) Math.floor(cached * 1.1);
            if (roomCount > maxAllowed) {
                return false;
            }
        }

        if (allCached) {
            log.debug("Availability check served entirely from cache");
            return true;
        }

        // Cache miss - query database and populate cache
        List<RoomTypeInventory> inventories = inventoryRepository.findInventoryForDateRange(
                hotelId, roomTypeId, startDate, endDate.minusDays(1)
        );

        if (inventories.isEmpty()) {
            return false;
        }

        // Check availability and populate cache
        for (RoomTypeInventory inventory : inventories) {
            int available = inventory.getTotalInventory() - inventory.getTotalReserved();
            cacheService.updateAvailableRooms(
                    hotelId, roomTypeId, inventory.getDate(), available
            );

            if (!inventory.hasAvailability(roomCount)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Update inventory and cache atomically
     */
    @Transactional
    public void reserveRooms(Long hotelId, Long roomTypeId,
                            LocalDate startDate, LocalDate endDate,
                            int roomCount) {
        // Update database
        List<RoomTypeInventory> inventories = inventoryRepository.findInventoryForDateRangeWithLock(
                hotelId, roomTypeId, startDate, endDate.minusDays(1)
        );

        for (RoomTypeInventory inventory : inventories) {
            inventory.setTotalReserved(inventory.getTotalReserved() + roomCount);
            inventoryRepository.save(inventory);

            // Update cache
            int available = inventory.getTotalInventory() - inventory.getTotalReserved();
            cacheService.updateAvailableRooms(hotelId, roomTypeId, inventory.getDate(), available);
        }
    }

    /**
     * Release rooms back to inventory (for cancellations)
     */
    @Transactional
    public void releaseRooms(Long hotelId, Long roomTypeId,
                            LocalDate startDate, LocalDate endDate,
                            int roomCount) {
        // Update database
        List<RoomTypeInventory> inventories = inventoryRepository.findInventoryForDateRange(
                hotelId, roomTypeId, startDate, endDate.minusDays(1)
        );

        for (RoomTypeInventory inventory : inventories) {
            inventory.setTotalReserved(inventory.getTotalReserved() - roomCount);
            inventoryRepository.save(inventory);

            // Update cache
            int available = inventory.getTotalInventory() - inventory.getTotalReserved();
            cacheService.updateAvailableRooms(hotelId, roomTypeId, inventory.getDate(), available);
        }
    }

    /**
     * Warm up cache for a hotel's inventory
     */
    public void warmUpCache(Long hotelId, Long roomTypeId, int daysAhead) {
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(daysAhead);

        List<RoomTypeInventory> inventories = inventoryRepository.findInventoryForDateRange(
                hotelId, roomTypeId, startDate, endDate
        );

        for (RoomTypeInventory inventory : inventories) {
            int available = inventory.getTotalInventory() - inventory.getTotalReserved();
            cacheService.updateAvailableRooms(hotelId, roomTypeId, inventory.getDate(), available);
        }

        log.info("Warmed up cache for hotel {} room type {} with {} days of inventory",
                hotelId, roomTypeId, inventories.size());
    }

    /**
     * Invalidate cache for a date range (force refresh from database)
     */
    public void invalidateCache(Long hotelId, Long roomTypeId,
                               LocalDate startDate, LocalDate endDate) {
        cacheService.invalidateInventory(hotelId, roomTypeId, startDate, endDate);
    }
}
