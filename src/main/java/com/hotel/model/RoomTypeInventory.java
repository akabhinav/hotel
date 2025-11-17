package com.hotel.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "room_type_inventory")
@IdClass(RoomTypeInventory.RoomTypeInventoryId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeInventory {

    @Id
    @Column(name = "hotel_id")
    private Long hotelId;

    @Id
    @Column(name = "room_type_id")
    private Long roomTypeId;

    @Id
    @Column(name = "date")
    private LocalDate date;

    @NotNull(message = "Total inventory is required")
    @Min(value = 0, message = "Total inventory must be non-negative")
    @Column(nullable = false)
    private Integer totalInventory;

    @NotNull(message = "Total reserved is required")
    @Min(value = 0, message = "Total reserved must be non-negative")
    @Column(nullable = false)
    private Integer totalReserved = 0;

    @Version
    private Long version;  // For optimistic locking

    // Composite key class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomTypeInventoryId implements Serializable {
        private Long hotelId;
        private Long roomTypeId;
        private LocalDate date;
    }

    /**
     * Check if enough rooms are available (supports overbooking at 10%)
     */
    public boolean hasAvailability(int roomsRequested) {
        int maxAllowed = (int) Math.floor(totalInventory * 1.1); // 10% overbooking
        return (totalReserved + roomsRequested) <= maxAllowed;
    }

    /**
     * Get available room count
     */
    public int getAvailableRooms() {
        return totalInventory - totalReserved;
    }
}
