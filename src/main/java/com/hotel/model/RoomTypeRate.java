package com.hotel.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "room_type_rates")
@IdClass(RoomTypeRate.RoomTypeRateId.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeRate {

    @Id
    @Column(name = "hotel_id")
    private Long hotelId;

    @Id
    @Column(name = "room_type_id")
    private Long roomTypeId;

    @Id
    @Column(name = "date")
    private LocalDate date;

    @NotNull(message = "Rate is required")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal rate;

    // Composite key class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomTypeRateId implements Serializable {
        private Long hotelId;
        private Long roomTypeId;
        private LocalDate date;
    }
}
