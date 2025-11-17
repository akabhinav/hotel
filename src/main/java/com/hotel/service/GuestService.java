package com.hotel.service;

import com.hotel.dto.GuestRequest;
import com.hotel.exception.ResourceNotFoundException;
import com.hotel.model.Guest;
import com.hotel.repository.GuestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GuestService {

    private final GuestRepository guestRepository;

    public List<Guest> getAllGuests() {
        return guestRepository.findAll();
    }

    public Guest getGuestById(Long id) {
        return guestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with id: " + id));
    }

    public Guest getGuestByEmail(String email) {
        return guestRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Guest not found with email: " + email));
    }

    @Transactional
    public Guest createGuest(GuestRequest request) {
        // Check if email already exists
        guestRepository.findByEmail(request.getEmail())
                .ifPresent(g -> {
                    throw new IllegalArgumentException("Guest already exists with email: " + request.getEmail());
                });

        Guest guest = new Guest();
        guest.setFirstName(request.getFirstName());
        guest.setLastName(request.getLastName());
        guest.setEmail(request.getEmail());
        guest.setPhone(request.getPhone());

        return guestRepository.save(guest);
    }

    @Transactional
    public Guest updateGuest(Long id, GuestRequest request) {
        Guest guest = getGuestById(id);

        guest.setFirstName(request.getFirstName());
        guest.setLastName(request.getLastName());
        guest.setEmail(request.getEmail());
        guest.setPhone(request.getPhone());

        return guestRepository.save(guest);
    }

    @Transactional
    public void deleteGuest(Long id) {
        Guest guest = getGuestById(id);
        guestRepository.delete(guest);
    }
}
