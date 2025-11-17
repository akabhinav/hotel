package com.hotel.repository;

import com.hotel.model.RoomTypeInventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomTypeInventoryRepository extends JpaRepository<RoomTypeInventory, RoomTypeInventory.RoomTypeInventoryId> {

    @Query("SELECT i FROM RoomTypeInventory i WHERE i.hotelId = :hotelId " +
           "AND i.roomTypeId = :roomTypeId AND i.date BETWEEN :startDate AND :endDate " +
           "ORDER BY i.date")
    List<RoomTypeInventory> findInventoryForDateRange(
            @Param("hotelId") Long hotelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT i FROM RoomTypeInventory i WHERE i.hotelId = :hotelId " +
           "AND i.roomTypeId = :roomTypeId AND i.date BETWEEN :startDate AND :endDate " +
           "ORDER BY i.date")
    List<RoomTypeInventory> findInventoryForDateRangeWithLock(
            @Param("hotelId") Long hotelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query("UPDATE RoomTypeInventory i SET i.totalReserved = i.totalReserved + :count " +
           "WHERE i.hotelId = :hotelId AND i.roomTypeId = :roomTypeId " +
           "AND i.date BETWEEN :startDate AND :endDate")
    int updateReservedCount(
            @Param("hotelId") Long hotelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("count") int count
    );
}
