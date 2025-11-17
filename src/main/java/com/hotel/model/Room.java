package com.hotel.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rooms",
       uniqueConstraints = @UniqueConstraint(columnNames = {"hotel_id", "room_number"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long roomId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_type_id", nullable = false)
    private RoomType roomType;

    @NotNull(message = "Room number is required")
    @Column(name = "room_number", nullable = false)
    private String roomNumber;

    @NotNull(message = "Floor is required")
    private Integer floor;

    @Column(nullable = false)
    private Boolean isAvailable = true;

    private String notes;
}
