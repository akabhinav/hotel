package com.hotel.service;

import com.hotel.dto.RoomTypeRequest;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.model.Hotel;
import com.hotel.model.RoomType;
import com.hotel.repository.RoomTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoomTypeService {

    private final RoomTypeRepository roomTypeRepository;
    private final HotelService hotelService;

    public List<RoomType> getRoomTypesByHotel(Long hotelId) {
        return roomTypeRepository.findByHotelHotelId(hotelId);
    }

    public RoomType getRoomTypeById(Long hotelId, Long roomTypeId) {
        RoomType roomType = roomTypeRepository.findById(roomTypeId)
                .orElseThrow(() -> new ResourceNotFoundException("Room type not found with id: " + roomTypeId));

        if (!roomType.getHotel().getHotelId().equals(hotelId)) {
            throw new ResourceNotFoundException("Room type not found in hotel: " + hotelId);
        }

        return roomType;
    }

    @Transactional
    public RoomType createRoomType(Long hotelId, RoomTypeRequest request) {
        Hotel hotel = hotelService.getHotelById(hotelId);

        // Check if room type name already exists
        roomTypeRepository.findByHotelHotelIdAndName(hotelId, request.getName())
                .ifPresent(rt -> {
                    throw new IllegalArgumentException("Room type already exists: " + request.getName());
                });

        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        roomType.setName(request.getName());
        roomType.setDescription(request.getDescription());
        roomType.setCapacity(request.getCapacity());

        return roomTypeRepository.save(roomType);
    }

    @Transactional
    public RoomType updateRoomType(Long hotelId, Long roomTypeId, RoomTypeRequest request) {
        RoomType roomType = getRoomTypeById(hotelId, roomTypeId);

        roomType.setName(request.getName());
        roomType.setDescription(request.getDescription());
        roomType.setCapacity(request.getCapacity());

        return roomTypeRepository.save(roomType);
    }

    @Transactional
    public void deleteRoomType(Long hotelId, Long roomTypeId) {
        RoomType roomType = getRoomTypeById(hotelId, roomTypeId);
        roomTypeRepository.delete(roomType);
    }
}
