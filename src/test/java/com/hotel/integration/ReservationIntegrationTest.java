package com.hotel.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.dto.ReservationRequest;
import com.hotel.model.*;
import com.hotel.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReservationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private RoomTypeInventoryRepository inventoryRepository;

    @Autowired
    private RoomTypeRateRepository rateRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    private Hotel testHotel;
    private RoomType testRoomType;
    private Guest testGuest;

    @BeforeEach
    void setUp() {
        // Clear all data
        reservationRepository.deleteAll();
        inventoryRepository.deleteAll();
        rateRepository.deleteAll();
        guestRepository.deleteAll();
        roomTypeRepository.deleteAll();
        hotelRepository.deleteAll();

        // Create test data
        testHotel = new Hotel();
        testHotel.setName("Test Hotel");
        testHotel.setAddress("123 Test St");
        testHotel.setLocation("Test City");
        testHotel = hotelRepository.save(testHotel);

        testRoomType = new RoomType();
        testRoomType.setHotel(testHotel);
        testRoomType.setName("Standard Room");
        testRoomType.setCapacity(2);
        testRoomType = roomTypeRepository.save(testRoomType);

        testGuest = new Guest();
        testGuest.setFirstName("John");
        testGuest.setLastName("Doe");
        testGuest.setEmail("john.doe@test.com");
        testGuest = guestRepository.save(testGuest);

        // Create inventory and rates for next 7 days
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate date = today.plusDays(i);

            RoomTypeInventory inventory = new RoomTypeInventory();
            inventory.setHotelId(testHotel.getHotelId());
            inventory.setRoomTypeId(testRoomType.getRoomTypeId());
            inventory.setDate(date);
            inventory.setTotalInventory(100);
            inventory.setTotalReserved(0);
            inventoryRepository.save(inventory);

            RoomTypeRate rate = new RoomTypeRate();
            rate.setHotelId(testHotel.getHotelId());
            rate.setRoomTypeId(testRoomType.getRoomTypeId());
            rate.setDate(date);
            rate.setRate(new BigDecimal("100.00"));
            rateRepository.save(rate);
        }
    }

    @Test
    void testCreateReservation_Success() throws Exception {
        ReservationRequest request = new ReservationRequest();
        request.setReservationId(UUID.randomUUID().toString());
        request.setHotelId(testHotel.getHotelId());
        request.setRoomTypeId(testRoomType.getRoomTypeId());
        request.setGuestId(testGuest.getGuestId());
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setRoomCount(2);

        mockMvc.perform(post("/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reservationId").value(request.getReservationId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.roomCount").value(2))
                .andExpect(jsonPath("$.totalPrice").value(200.00));
    }

    @Test
    void testCreateReservation_DuplicateIdempotencyKey_ReturnsConflict() throws Exception {
        String reservationId = UUID.randomUUID().toString();

        ReservationRequest request = new ReservationRequest();
        request.setReservationId(reservationId);
        request.setHotelId(testHotel.getHotelId());
        request.setRoomTypeId(testRoomType.getRoomTypeId());
        request.setGuestId(testGuest.getGuestId());
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setRoomCount(1);

        // First request should succeed
        mockMvc.perform(post("/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second request with same ID should fail
        mockMvc.perform(post("/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("already exists")));
    }

    @Test
    void testCheckAvailability_Success() throws Exception {
        mockMvc.perform(post("/v1/reservations/check-availability")
                        .param("hotelId", testHotel.getHotelId().toString())
                        .param("roomTypeId", testRoomType.getRoomTypeId().toString())
                        .param("startDate", LocalDate.now().plusDays(1).toString())
                        .param("endDate", LocalDate.now().plusDays(3).toString())
                        .param("roomCount", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.totalPrice").exists());
    }

    @Test
    void testCheckAvailability_InsufficientRooms_ReturnsFalse() throws Exception {
        // Book 99 rooms first
        for (int i = 0; i < 2; i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            RoomTypeInventory inventory = inventoryRepository
                    .findById(new RoomTypeInventory.RoomTypeInventoryId(
                            testHotel.getHotelId(),
                            testRoomType.getRoomTypeId(),
                            date))
                    .orElseThrow();
            inventory.setTotalReserved(99);
            inventoryRepository.save(inventory);
        }

        // Try to book 12 more rooms (total would be 111, which exceeds 110% of 100)
        mockMvc.perform(post("/v1/reservations/check-availability")
                        .param("hotelId", testHotel.getHotelId().toString())
                        .param("roomTypeId", testRoomType.getRoomTypeId().toString())
                        .param("startDate", LocalDate.now().toString())
                        .param("endDate", LocalDate.now().plusDays(2).toString())
                        .param("roomCount", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void testCancelReservation_Success() throws Exception {
        // Create a reservation first
        ReservationRequest request = new ReservationRequest();
        request.setReservationId(UUID.randomUUID().toString());
        request.setHotelId(testHotel.getHotelId());
        request.setRoomTypeId(testRoomType.getRoomTypeId());
        request.setGuestId(testGuest.getGuestId());
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setRoomCount(1);

        mockMvc.perform(post("/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Cancel it
        mockMvc.perform(delete("/v1/reservations/" + request.getReservationId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"));
    }

    @Test
    void testMarkReservationAsPaid_Success() throws Exception {
        // Create a reservation first
        ReservationRequest request = new ReservationRequest();
        request.setReservationId(UUID.randomUUID().toString());
        request.setHotelId(testHotel.getHotelId());
        request.setRoomTypeId(testRoomType.getRoomTypeId());
        request.setGuestId(testGuest.getGuestId());
        request.setStartDate(LocalDate.now().plusDays(1));
        request.setEndDate(LocalDate.now().plusDays(3));
        request.setRoomCount(1);

        mockMvc.perform(post("/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Mark as paid
        mockMvc.perform(post("/v1/reservations/" + request.getReservationId() + "/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    void testGetAllReservationsByGuest() throws Exception {
        // Create two reservations for the same guest
        for (int i = 0; i < 2; i++) {
            ReservationRequest request = new ReservationRequest();
            request.setReservationId(UUID.randomUUID().toString());
            request.setHotelId(testHotel.getHotelId());
            request.setRoomTypeId(testRoomType.getRoomTypeId());
            request.setGuestId(testGuest.getGuestId());
            request.setStartDate(LocalDate.now().plusDays(1 + i));
            request.setEndDate(LocalDate.now().plusDays(3 + i));
            request.setRoomCount(1);

            mockMvc.perform(post("/v1/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Get all reservations for the guest
        mockMvc.perform(get("/v1/reservations")
                        .param("guestId", testGuest.getGuestId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }
}
