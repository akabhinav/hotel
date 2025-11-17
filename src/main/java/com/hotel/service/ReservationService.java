package com.hotel.service;

import com.hotel.dto.ReservationRequest;
import com.hotel.exception.DuplicateReservationException;
import com.hotel.exception.InsufficientInventoryException;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.model.*;
import com.hotel.repository.ReservationRepository;
import com.hotel.repository.RoomTypeInventoryRepository;
import com.hotel.repository.RoomTypeRateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomTypeInventoryRepository inventoryRepository;
    private final RoomTypeRateRepository rateRepository;
    private final HotelService hotelService;
    private final RoomTypeService roomTypeService;
    private final GuestService guestService;

    public List<Reservation> getAllReservations() {
        return reservationRepository.findAll();
    }

    public Reservation getReservationById(String reservationId) {
        return reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + reservationId));
    }

    public List<Reservation> getReservationsByGuest(Long guestId) {
        return reservationRepository.findByGuestGuestId(guestId);
    }

    public List<Reservation> getReservationsByHotel(Long hotelId) {
        return reservationRepository.findByHotelHotelId(hotelId);
    }

    /**
     * Check room availability for a given date range.
     * Supports 10% overbooking.
     */
    public boolean checkAvailability(Long hotelId, Long roomTypeId, LocalDate startDate,
                                    LocalDate endDate, int roomCount) {
        List<RoomTypeInventory> inventories = inventoryRepository.findInventoryForDateRange(
                hotelId, roomTypeId, startDate, endDate.minusDays(1)
        );

        if (inventories.isEmpty()) {
            return false;
        }

        // Check if all dates in range have sufficient inventory
        return inventories.stream()
                .allMatch(inv -> inv.hasAvailability(roomCount));
    }

    /**
     * Calculate total price for a reservation based on dynamic rates
     */
    public BigDecimal calculateTotalPrice(Long hotelId, Long roomTypeId, LocalDate startDate,
                                         LocalDate endDate, int roomCount) {
        List<RoomTypeRate> rates = rateRepository.findRatesForDateRange(
                hotelId, roomTypeId, startDate, endDate.minusDays(1)
        );

        if (rates.isEmpty()) {
            throw new IllegalArgumentException("No rates found for the specified date range");
        }

        return rates.stream()
                .map(RoomTypeRate::getRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .multiply(BigDecimal.valueOf(roomCount));
    }

    /**
     * Create a reservation with optimistic locking for concurrency control.
     * Uses reservationId as idempotency key to prevent double booking.
     */
    @Transactional
    @Retryable(
        retryFor = {OptimisticLockingFailureException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    public Reservation createReservation(ReservationRequest request) {
        // Validate date range
        if (request.getEndDate().isBefore(request.getStartDate()) ||
            request.getEndDate().equals(request.getStartDate())) {
            throw new IllegalArgumentException("End date must be after start date");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Cannot book dates in the past");
        }

        // Check for duplicate reservation (idempotency)
        if (reservationRepository.existsById(request.getReservationId())) {
            throw new DuplicateReservationException(
                "Reservation already exists with ID: " + request.getReservationId()
            );
        }

        // Validate entities exist
        Hotel hotel = hotelService.getHotelById(request.getHotelId());
        RoomType roomType = roomTypeService.getRoomTypeById(request.getHotelId(), request.getRoomTypeId());
        Guest guest = guestService.getGuestById(request.getGuestId());

        // Check inventory with optimistic locking
        List<RoomTypeInventory> inventories = inventoryRepository.findInventoryForDateRangeWithLock(
                request.getHotelId(),
                request.getRoomTypeId(),
                request.getStartDate(),
                request.getEndDate().minusDays(1)
        );

        // Validate inventory exists for all dates
        long expectedDays = request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay();
        if (inventories.size() != expectedDays) {
            throw new IllegalArgumentException(
                "Inventory not found for all dates in range. Expected " + expectedDays +
                " days, found " + inventories.size()
            );
        }

        // Check availability for each date (with 10% overbooking support)
        for (RoomTypeInventory inventory : inventories) {
            if (!inventory.hasAvailability(request.getRoomCount())) {
                throw new InsufficientInventoryException(
                    "Insufficient rooms available for date: " + inventory.getDate() +
                    " (Available: " + inventory.getAvailableRooms() +
                    ", Requested: " + request.getRoomCount() + ")"
                );
            }
        }

        // Update inventory (reserve rooms)
        for (RoomTypeInventory inventory : inventories) {
            inventory.setTotalReserved(inventory.getTotalReserved() + request.getRoomCount());
            inventoryRepository.save(inventory);
        }

        // Calculate total price
        BigDecimal totalPrice = calculateTotalPrice(
                request.getHotelId(),
                request.getRoomTypeId(),
                request.getStartDate(),
                request.getEndDate(),
                request.getRoomCount()
        );

        // Create reservation
        Reservation reservation = new Reservation();
        reservation.setReservationId(request.getReservationId());
        reservation.setHotel(hotel);
        reservation.setRoomType(roomType);
        reservation.setGuest(guest);
        reservation.setStartDate(request.getStartDate());
        reservation.setEndDate(request.getEndDate());
        reservation.setRoomCount(request.getRoomCount());
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setTotalPrice(totalPrice);

        try {
            return reservationRepository.save(reservation);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateReservationException(
                "Reservation already exists with ID: " + request.getReservationId()
            );
        }
    }

    /**
     * Cancel a reservation and release inventory
     */
    @Transactional
    public Reservation cancelReservation(String reservationId) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() == ReservationStatus.CANCELED) {
            throw new IllegalArgumentException("Reservation is already canceled");
        }

        if (reservation.getStatus() == ReservationStatus.REFUNDED) {
            throw new IllegalArgumentException("Reservation is already refunded");
        }

        // Release inventory
        List<RoomTypeInventory> inventories = inventoryRepository.findInventoryForDateRange(
                reservation.getHotel().getHotelId(),
                reservation.getRoomType().getRoomTypeId(),
                reservation.getStartDate(),
                reservation.getEndDate().minusDays(1)
        );

        for (RoomTypeInventory inventory : inventories) {
            inventory.setTotalReserved(inventory.getTotalReserved() - reservation.getRoomCount());
            inventoryRepository.save(inventory);
        }

        // Update reservation status
        if (reservation.getStatus() == ReservationStatus.PAID) {
            reservation.setStatus(ReservationStatus.REFUNDED);
        } else {
            reservation.setStatus(ReservationStatus.CANCELED);
        }

        return reservationRepository.save(reservation);
    }

    /**
     * Update reservation status to PAID
     */
    @Transactional
    public Reservation markAsPaid(String reservationId) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Only pending reservations can be marked as paid");
        }

        reservation.setStatus(ReservationStatus.PAID);
        return reservationRepository.save(reservation);
    }

    /**
     * Reject a reservation and release inventory
     */
    @Transactional
    public Reservation rejectReservation(String reservationId) {
        Reservation reservation = getReservationById(reservationId);

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("Only pending reservations can be rejected");
        }

        // Release inventory
        List<RoomTypeInventory> inventories = inventoryRepository.findInventoryForDateRange(
                reservation.getHotel().getHotelId(),
                reservation.getRoomType().getRoomTypeId(),
                reservation.getStartDate(),
                reservation.getEndDate().minusDays(1)
        );

        for (RoomTypeInventory inventory : inventories) {
            inventory.setTotalReserved(inventory.getTotalReserved() - reservation.getRoomCount());
            inventoryRepository.save(inventory);
        }

        reservation.setStatus(ReservationStatus.REJECTED);
        return reservationRepository.save(reservation);
    }
}
