package com.hotel.service;

import com.hotel.dto.RoomRequest;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.model.Hotel;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelService hotelService;
    private final RoomTypeService roomTypeService;

    public List<Room> getRoomsByHotel(Long hotelId) {
        return roomRepository.findByHotelHotelId(hotelId);
    }

    public Room getRoomById(Long hotelId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + roomId));

        if (!room.getHotel().getHotelId().equals(hotelId)) {
            throw new ResourceNotFoundException("Room not found in hotel: " + hotelId);
        }

        return room;
    }

    @Transactional
    public Room createRoom(Long hotelId, RoomRequest request) {
        Hotel hotel = hotelService.getHotelById(hotelId);
        RoomType roomType = roomTypeService.getRoomTypeById(hotelId, request.getRoomTypeId());

        // Check if room number already exists
        roomRepository.findByHotelHotelIdAndRoomNumber(hotelId, request.getRoomNumber())
                .ifPresent(r -> {
                    throw new IllegalArgumentException("Room number already exists: " + request.getRoomNumber());
                });

        Room room = new Room();
        room.setHotel(hotel);
        room.setRoomType(roomType);
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setIsAvailable(request.getIsAvailable());
        room.setNotes(request.getNotes());

        return roomRepository.save(room);
    }

    @Transactional
    public Room updateRoom(Long hotelId, Long roomId, RoomRequest request) {
        Room room = getRoomById(hotelId, roomId);
        RoomType roomType = roomTypeService.getRoomTypeById(hotelId, request.getRoomTypeId());

        room.setRoomType(roomType);
        room.setRoomNumber(request.getRoomNumber());
        room.setFloor(request.getFloor());
        room.setIsAvailable(request.getIsAvailable());
        room.setNotes(request.getNotes());

        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long hotelId, Long roomId) {
        Room room = getRoomById(hotelId, roomId);
        roomRepository.delete(room);
    }
}
