package com.hotel.repository;

import com.hotel.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByHotelHotelId(Long hotelId);
    List<Room> findByHotelHotelIdAndRoomTypeRoomTypeId(Long hotelId, Long roomTypeId);
    Optional<Room> findByHotelHotelIdAndRoomNumber(Long hotelId, String roomNumber);
}
