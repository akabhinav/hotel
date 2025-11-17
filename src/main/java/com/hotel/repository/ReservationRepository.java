package com.hotel.repository;

import com.hotel.model.Reservation;
import com.hotel.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, String> {
    List<Reservation> findByGuestGuestId(Long guestId);
    List<Reservation> findByHotelHotelId(Long hotelId);
    List<Reservation> findByStatus(ReservationStatus status);
    List<Reservation> findByGuestGuestIdAndStatus(Long guestId, ReservationStatus status);
}
