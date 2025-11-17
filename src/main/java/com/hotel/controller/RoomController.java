package com.hotel.controller;

import com.hotel.dto.RoomRequest;
import com.hotel.dto.RoomTypeRequest;
import com.hotel.model.Room;
import com.hotel.model.RoomType;
import com.hotel.service.RoomService;
import com.hotel.service.RoomTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/hotels/{hotelId}")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomTypeService roomTypeService;

    // Room Type endpoints
    @GetMapping("/room-types")
    public ResponseEntity<List<RoomType>> getRoomTypes(@PathVariable Long hotelId) {
        List<RoomType> roomTypes = roomTypeService.getRoomTypesByHotel(hotelId);
        return ResponseEntity.ok(roomTypes);
    }

    @GetMapping("/room-types/{roomTypeId}")
    public ResponseEntity<RoomType> getRoomType(
            @PathVariable Long hotelId,
            @PathVariable Long roomTypeId) {
        RoomType roomType = roomTypeService.getRoomTypeById(hotelId, roomTypeId);
        return ResponseEntity.ok(roomType);
    }

    @PostMapping("/room-types")
    public ResponseEntity<RoomType> createRoomType(
            @PathVariable Long hotelId,
            @Valid @RequestBody RoomTypeRequest request) {
        RoomType roomType = roomTypeService.createRoomType(hotelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(roomType);
    }

    @PutMapping("/room-types/{roomTypeId}")
    public ResponseEntity<RoomType> updateRoomType(
            @PathVariable Long hotelId,
            @PathVariable Long roomTypeId,
            @Valid @RequestBody RoomTypeRequest request) {
        RoomType roomType = roomTypeService.updateRoomType(hotelId, roomTypeId, request);
        return ResponseEntity.ok(roomType);
    }

    @DeleteMapping("/room-types/{roomTypeId}")
    public ResponseEntity<Void> deleteRoomType(
            @PathVariable Long hotelId,
            @PathVariable Long roomTypeId) {
        roomTypeService.deleteRoomType(hotelId, roomTypeId);
        return ResponseEntity.noContent().build();
    }

    // Room endpoints
    @GetMapping("/rooms")
    public ResponseEntity<List<Room>> getRooms(@PathVariable Long hotelId) {
        List<Room> rooms = roomService.getRoomsByHotel(hotelId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<Room> getRoom(
            @PathVariable Long hotelId,
            @PathVariable Long roomId) {
        Room room = roomService.getRoomById(hotelId, roomId);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/rooms")
    public ResponseEntity<Room> createRoom(
            @PathVariable Long hotelId,
            @Valid @RequestBody RoomRequest request) {
        Room room = roomService.createRoom(hotelId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<Room> updateRoom(
            @PathVariable Long hotelId,
            @PathVariable Long roomId,
            @Valid @RequestBody RoomRequest request) {
        Room room = roomService.updateRoom(hotelId, roomId, request);
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(
            @PathVariable Long hotelId,
            @PathVariable Long roomId) {
        roomService.deleteRoom(hotelId, roomId);
        return ResponseEntity.noContent().build();
    }
}
