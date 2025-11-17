package com.hotel.service;

import com.hotel.dto.ReservationRequest;
import com.hotel.exception.DuplicateReservationException;
import com.hotel.exception.InsufficientInventoryException;
import com.hotel.model.*;
import com.hotel.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RoomTypeInventoryRepository inventoryRepository;

    @Mock
    private RoomTypeRateRepository rateRepository;

    @Mock
    private HotelService hotelService;

    @Mock
    private RoomTypeService roomTypeService;

    @Mock
    private GuestService guestService;

    @InjectMocks
    private ReservationService reservationService;

    private Hotel testHotel;
    private RoomType testRoomType;
    private Guest testGuest;
    private ReservationRequest testRequest;

    @BeforeEach
    void setUp() {
        testHotel = new Hotel();
        testHotel.setHotelId(1L);
        testHotel.setName("Test Hotel");

        testRoomType = new RoomType();
        testRoomType.setRoomTypeId(1L);
        testRoomType.setName("Standard Room");
        testRoomType.setHotel(testHotel);

        testGuest = new Guest();
        testGuest.setGuestId(1L);
        testGuest.setEmail("test@email.com");

        testRequest = new ReservationRequest();
        testRequest.setReservationId("RES-001");
        testRequest.setHotelId(1L);
        testRequest.setRoomTypeId(1L);
        testRequest.setGuestId(1L);
        testRequest.setStartDate(LocalDate.now().plusDays(1));
        testRequest.setEndDate(LocalDate.now().plusDays(3));
        testRequest.setRoomCount(1);
    }

    @Test
    void testCheckAvailability_WhenRoomsAvailable_ReturnsTrue() {
        // Arrange
        RoomTypeInventory inv1 = createInventory(LocalDate.now().plusDays(1), 100, 80);
        RoomTypeInventory inv2 = createInventory(LocalDate.now().plusDays(2), 100, 80);

        when(inventoryRepository.findInventoryForDateRange(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(inv1, inv2));

        // Act
        boolean result = reservationService.checkAvailability(
                1L, 1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 5
        );

        // Assert
        assertTrue(result);
    }

    @Test
    void testCheckAvailability_WhenInsufficientRooms_ReturnsFalse() {
        // Arrange
        RoomTypeInventory inv1 = createInventory(LocalDate.now().plusDays(1), 100, 99);
        RoomTypeInventory inv2 = createInventory(LocalDate.now().plusDays(2), 100, 80);

        when(inventoryRepository.findInventoryForDateRange(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(inv1, inv2));

        // Act
        boolean result = reservationService.checkAvailability(
                1L, 1L, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), 12
        );

        // Assert
        assertFalse(result);
    }

    @Test
    void testCreateReservation_Success() {
        // Arrange
        when(reservationRepository.existsById(anyString())).thenReturn(false);
        when(hotelService.getHotelById(anyLong())).thenReturn(testHotel);
        when(roomTypeService.getRoomTypeById(anyLong(), anyLong())).thenReturn(testRoomType);
        when(guestService.getGuestById(anyLong())).thenReturn(testGuest);

        RoomTypeInventory inv1 = createInventory(testRequest.getStartDate(), 100, 80);
        RoomTypeInventory inv2 = createInventory(testRequest.getStartDate().plusDays(1), 100, 80);

        when(inventoryRepository.findInventoryForDateRangeWithLock(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(inv1, inv2));

        RoomTypeRate rate1 = createRate(testRequest.getStartDate(), new BigDecimal("100.00"));
        RoomTypeRate rate2 = createRate(testRequest.getStartDate().plusDays(1), new BigDecimal("100.00"));

        when(rateRepository.findRatesForDateRange(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(rate1, rate2));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Reservation result = reservationService.createReservation(testRequest);

        // Assert
        assertNotNull(result);
        assertEquals(testRequest.getReservationId(), result.getReservationId());
        assertEquals(ReservationStatus.PENDING, result.getStatus());
        verify(inventoryRepository, times(2)).save(any(RoomTypeInventory.class));
    }

    @Test
    void testCreateReservation_DuplicateReservation_ThrowsException() {
        // Arrange
        when(reservationRepository.existsById(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(DuplicateReservationException.class, () ->
                reservationService.createReservation(testRequest)
        );

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void testCreateReservation_InsufficientInventory_ThrowsException() {
        // Arrange
        when(reservationRepository.existsById(anyString())).thenReturn(false);
        when(hotelService.getHotelById(anyLong())).thenReturn(testHotel);
        when(roomTypeService.getRoomTypeById(anyLong(), anyLong())).thenReturn(testRoomType);
        when(guestService.getGuestById(anyLong())).thenReturn(testGuest);

        RoomTypeInventory inv1 = createInventory(testRequest.getStartDate(), 100, 110);
        RoomTypeInventory inv2 = createInventory(testRequest.getStartDate().plusDays(1), 100, 80);

        when(inventoryRepository.findInventoryForDateRangeWithLock(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(inv1, inv2));

        // Act & Assert
        assertThrows(InsufficientInventoryException.class, () ->
                reservationService.createReservation(testRequest)
        );

        verify(reservationRepository, never()).save(any());
    }

    @Test
    void testCancelReservation_Success() {
        // Arrange
        Reservation reservation = new Reservation();
        reservation.setReservationId("RES-001");
        reservation.setHotel(testHotel);
        reservation.setRoomType(testRoomType);
        reservation.setStatus(ReservationStatus.PENDING);
        reservation.setStartDate(LocalDate.now().plusDays(1));
        reservation.setEndDate(LocalDate.now().plusDays(3));
        reservation.setRoomCount(2);

        when(reservationRepository.findById(anyString())).thenReturn(Optional.of(reservation));

        RoomTypeInventory inv1 = createInventory(reservation.getStartDate(), 100, 80);
        RoomTypeInventory inv2 = createInventory(reservation.getStartDate().plusDays(1), 100, 80);

        when(inventoryRepository.findInventoryForDateRange(any(), any(), any(), any()))
                .thenReturn(Arrays.asList(inv1, inv2));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Reservation result = reservationService.cancelReservation("RES-001");

        // Assert
        assertNotNull(result);
        assertEquals(ReservationStatus.CANCELED, result.getStatus());
        verify(inventoryRepository, times(2)).save(any(RoomTypeInventory.class));
    }

    @Test
    void testMarkAsPaid_Success() {
        // Arrange
        Reservation reservation = new Reservation();
        reservation.setReservationId("RES-001");
        reservation.setStatus(ReservationStatus.PENDING);

        when(reservationRepository.findById(anyString())).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Reservation result = reservationService.markAsPaid("RES-001");

        // Assert
        assertNotNull(result);
        assertEquals(ReservationStatus.PAID, result.getStatus());
    }

    private RoomTypeInventory createInventory(LocalDate date, int total, int reserved) {
        RoomTypeInventory inventory = new RoomTypeInventory();
        inventory.setHotelId(1L);
        inventory.setRoomTypeId(1L);
        inventory.setDate(date);
        inventory.setTotalInventory(total);
        inventory.setTotalReserved(reserved);
        return inventory;
    }

    private RoomTypeRate createRate(LocalDate date, BigDecimal rate) {
        RoomTypeRate roomRate = new RoomTypeRate();
        roomRate.setHotelId(1L);
        roomRate.setRoomTypeId(1L);
        roomRate.setDate(date);
        roomRate.setRate(rate);
        return roomRate;
    }
}
