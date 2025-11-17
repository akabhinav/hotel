package com.hotel.config;

import com.hotel.model.*;
import com.hotel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomTypeRepository roomTypeRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final RoomTypeRateRepository rateRepository;
    private final RoomTypeInventoryRepository inventoryRepository;

    @Override
    public void run(String... args) {
        log.info("Loading sample data...");

        // Create Hotels
        Hotel marriott = createHotel("San Francisco Marriott Marquis",
                "780 Mission St", "San Francisco, CA");
        Hotel hilton = createHotel("Hilton San Francisco Union Square",
                "333 O'Farrell St", "San Francisco, CA");
        Hotel hyatt = createHotel("Grand Hyatt San Francisco",
                "345 Stockton St", "San Francisco, CA");

        // Create Room Types for Marriott
        RoomType marriottStandard = createRoomType(marriott, "Standard Room",
                "Comfortable room with city view", 2);
        RoomType marriottKing = createRoomType(marriott, "King Room",
                "Spacious room with king-size bed", 2);
        RoomType marriottSuite = createRoomType(marriott, "Executive Suite",
                "Luxury suite with separate living area", 4);

        // Create Room Types for Hilton
        RoomType hiltonStandard = createRoomType(hilton, "Standard Room",
                "Cozy room with modern amenities", 2);
        RoomType hiltonDeluxe = createRoomType(hilton, "Deluxe Room",
                "Enhanced room with premium features", 3);

        // Create Rooms
        createRooms(marriott, marriottStandard, 100, 1, 10);  // 100 standard rooms
        createRooms(marriott, marriottKing, 150, 11, 20);     // 150 king rooms
        createRooms(marriott, marriottSuite, 50, 21, 25);     // 50 suites

        createRooms(hilton, hiltonStandard, 120, 1, 12);      // 120 standard rooms
        createRooms(hilton, hiltonDeluxe, 80, 13, 20);        // 80 deluxe rooms

        // Create Guests
        createGuest("John", "Doe", "john.doe@email.com", "+1-555-0101");
        createGuest("Jane", "Smith", "jane.smith@email.com", "+1-555-0102");
        createGuest("Bob", "Johnson", "bob.johnson@email.com", "+1-555-0103");
        createGuest("Alice", "Williams", "alice.williams@email.com", "+1-555-0104");
        createGuest("Charlie", "Brown", "charlie.brown@email.com", "+1-555-0105");

        // Create Rates and Inventory for the next 365 days
        LocalDate startDate = LocalDate.now();
        createRatesAndInventory(marriott.getHotelId(), marriottStandard.getRoomTypeId(),
                startDate, 365, new BigDecimal("150.00"), 100);
        createRatesAndInventory(marriott.getHotelId(), marriottKing.getRoomTypeId(),
                startDate, 365, new BigDecimal("200.00"), 150);
        createRatesAndInventory(marriott.getHotelId(), marriottSuite.getRoomTypeId(),
                startDate, 365, new BigDecimal("350.00"), 50);

        createRatesAndInventory(hilton.getHotelId(), hiltonStandard.getRoomTypeId(),
                startDate, 365, new BigDecimal("140.00"), 120);
        createRatesAndInventory(hilton.getHotelId(), hiltonDeluxe.getRoomTypeId(),
                startDate, 365, new BigDecimal("190.00"), 80);

        log.info("Sample data loaded successfully!");
    }

    private Hotel createHotel(String name, String address, String location) {
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress(address);
        hotel.setLocation(location);
        hotel.setDescription("Premium hotel with excellent service");
        return hotelRepository.save(hotel);
    }

    private RoomType createRoomType(Hotel hotel, String name, String description, int capacity) {
        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        roomType.setName(name);
        roomType.setDescription(description);
        roomType.setCapacity(capacity);
        return roomTypeRepository.save(roomType);
    }

    private void createRooms(Hotel hotel, RoomType roomType, int count,
                            int startFloor, int endFloor) {
        int roomsPerFloor = count / (endFloor - startFloor + 1);
        int roomNumber = 1;

        for (int floor = startFloor; floor <= endFloor; floor++) {
            for (int i = 0; i < roomsPerFloor; i++) {
                Room room = new Room();
                room.setHotel(hotel);
                room.setRoomType(roomType);
                room.setRoomNumber(floor + String.format("%02d", roomNumber++));
                room.setFloor(floor);
                room.setIsAvailable(true);
                roomRepository.save(room);
            }
        }
    }

    private Guest createGuest(String firstName, String lastName, String email, String phone) {
        Guest guest = new Guest();
        guest.setFirstName(firstName);
        guest.setLastName(lastName);
        guest.setEmail(email);
        guest.setPhone(phone);
        return guestRepository.save(guest);
    }

    private void createRatesAndInventory(Long hotelId, Long roomTypeId,
                                        LocalDate startDate, int days,
                                        BigDecimal baseRate, int totalInventory) {
        List<RoomTypeRate> rates = new ArrayList<>();
        List<RoomTypeInventory> inventories = new ArrayList<>();

        for (int i = 0; i < days; i++) {
            LocalDate date = startDate.plusDays(i);

            // Vary rates slightly based on day of week (weekends are more expensive)
            BigDecimal rate = baseRate;
            int dayOfWeek = date.getDayOfWeek().getValue();
            if (dayOfWeek >= 5) { // Friday, Saturday, Sunday
                rate = rate.multiply(new BigDecimal("1.2"));
            }

            // Create rate
            RoomTypeRate roomRate = new RoomTypeRate();
            roomRate.setHotelId(hotelId);
            roomRate.setRoomTypeId(roomTypeId);
            roomRate.setDate(date);
            roomRate.setRate(rate);
            rates.add(roomRate);

            // Create inventory
            RoomTypeInventory inventory = new RoomTypeInventory();
            inventory.setHotelId(hotelId);
            inventory.setRoomTypeId(roomTypeId);
            inventory.setDate(date);
            inventory.setTotalInventory(totalInventory);
            inventory.setTotalReserved(0);
            inventories.add(inventory);
        }

        rateRepository.saveAll(rates);
        inventoryRepository.saveAll(inventories);
    }
}
