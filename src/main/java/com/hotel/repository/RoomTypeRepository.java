package com.hotel.repository;

import com.hotel.model.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTypeRepository extends JpaRepository<RoomType, Long> {
    List<RoomType> findByHotelHotelId(Long hotelId);
    Optional<RoomType> findByHotelHotelIdAndName(Long hotelId, String name);
}
