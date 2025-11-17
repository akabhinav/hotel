package com.hotel.controller;

import com.hotel.dto.GuestRequest;
import com.hotel.model.Guest;
import com.hotel.service.GuestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/guests")
@RequiredArgsConstructor
public class GuestController {

    private final GuestService guestService;

    @GetMapping
    public ResponseEntity<List<Guest>> getAllGuests() {
        List<Guest> guests = guestService.getAllGuests();
        return ResponseEntity.ok(guests);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Guest> getGuest(@PathVariable Long id) {
        Guest guest = guestService.getGuestById(id);
        return ResponseEntity.ok(guest);
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Guest> getGuestByEmail(@PathVariable String email) {
        Guest guest = guestService.getGuestByEmail(email);
        return ResponseEntity.ok(guest);
    }

    @PostMapping
    public ResponseEntity<Guest> createGuest(@Valid @RequestBody GuestRequest request) {
        Guest guest = guestService.createGuest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(guest);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Guest> updateGuest(
            @PathVariable Long id,
            @Valid @RequestBody GuestRequest request) {
        Guest guest = guestService.updateGuest(id, request);
        return ResponseEntity.ok(guest);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGuest(@PathVariable Long id) {
        guestService.deleteGuest(id);
        return ResponseEntity.noContent().build();
    }
}
