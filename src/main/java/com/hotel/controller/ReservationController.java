package com.hotel.controller;

import com.hotel.dto.ReservationRequest;
import com.hotel.model.Reservation;
import com.hotel.service.ReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/reservations")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationService reservationService;

    @GetMapping
    public ResponseEntity<List<Reservation>> getAllReservations(
            @RequestParam(required = false) Long guestId,
            @RequestParam(required = false) Long hotelId) {

        List<Reservation> reservations;
        if (guestId != null) {
            reservations = reservationService.getReservationsByGuest(guestId);
        } else if (hotelId != null) {
            reservations = reservationService.getReservationsByHotel(hotelId);
        } else {
            reservations = reservationService.getAllReservations();
        }

        return ResponseEntity.ok(reservations);
    }

    @GetMapping("/{reservationId}")
    public ResponseEntity<Reservation> getReservation(@PathVariable String reservationId) {
        Reservation reservation = reservationService.getReservationById(reservationId);
        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/check-availability")
    public ResponseEntity<Map<String, Object>> checkAvailability(
            @RequestParam Long hotelId,
            @RequestParam Long roomTypeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "1") int roomCount) {

        boolean available = reservationService.checkAvailability(
                hotelId, roomTypeId, startDate, endDate, roomCount
        );

        Map<String, Object> response = new HashMap<>();
        response.put("available", available);
        response.put("hotelId", hotelId);
        response.put("roomTypeId", roomTypeId);
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        response.put("roomCount", roomCount);

        if (available) {
            BigDecimal totalPrice = reservationService.calculateTotalPrice(
                    hotelId, roomTypeId, startDate, endDate, roomCount
            );
            response.put("totalPrice", totalPrice);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<Reservation> createReservation(
            @Valid @RequestBody ReservationRequest request) {
        Reservation reservation = reservationService.createReservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(reservation);
    }

    @DeleteMapping("/{reservationId}")
    public ResponseEntity<Reservation> cancelReservation(@PathVariable String reservationId) {
        Reservation reservation = reservationService.cancelReservation(reservationId);
        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/{reservationId}/pay")
    public ResponseEntity<Reservation> markAsPaid(@PathVariable String reservationId) {
        Reservation reservation = reservationService.markAsPaid(reservationId);
        return ResponseEntity.ok(reservation);
    }

    @PostMapping("/{reservationId}/reject")
    public ResponseEntity<Reservation> rejectReservation(@PathVariable String reservationId) {
        Reservation reservation = reservationService.rejectReservation(reservationId);
        return ResponseEntity.ok(reservation);
    }
}
