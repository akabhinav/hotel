package com.hotel.repository;

import com.hotel.model.RoomTypeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RoomTypeRateRepository extends JpaRepository<RoomTypeRate, RoomTypeRate.RoomTypeRateId> {

    @Query("SELECT r FROM RoomTypeRate r WHERE r.hotelId = :hotelId " +
           "AND r.roomTypeId = :roomTypeId AND r.date BETWEEN :startDate AND :endDate")
    List<RoomTypeRate> findRatesForDateRange(
            @Param("hotelId") Long hotelId,
            @Param("roomTypeId") Long roomTypeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );
}
