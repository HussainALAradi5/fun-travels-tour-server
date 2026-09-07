package com.server.server.controllers.tourmanagement;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.services.tourmanagement.TourReservationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class TourReservationController {
    private final TourReservationService reservationService;

    @PostMapping
    public ResponseEntity<TourReservation> create(@RequestBody TourReservation res) {
        return ResponseEntity.ok(reservationService.create(res));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TourReservation> updateStatus(@PathVariable Integer id, @RequestParam GenericStatus status) {
        return ResponseEntity.ok(reservationService.updateStatus(id, status));
    }


    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'EMPLOYEE')")
    public ResponseEntity<TourReservation> cancelReservation(@PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.cancelReservation(id));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<TourReservation>> filter(
            @RequestParam(required = false) GenericStatus status,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long agencyId) {
        return ResponseEntity.ok(reservationService.filter(status, customerId, agencyId));
    }

    @GetMapping
    public ResponseEntity<List<TourReservation>> getAll() {
        return ResponseEntity.ok(reservationService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TourReservation> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(reservationService.getById(id));
    }
}