package com.server.server.controllers;

import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.enums.Payment.PaymentStatus;
import com.server.server.models.Payment;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.services.PaymentService;
import com.server.server.services.tourmanagement.TourReservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final TourReservationService reservationService;

    @PostMapping("/execute/{reservationId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<TourReservation> executePayment(
            @PathVariable Integer reservationId,
            @RequestParam PaymentMethod method) {
        TourReservation result = reservationService.finalizeReservationWithPayment(reservationId, method);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<List<Payment>> getPayments(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        return ResponseEntity.ok(paymentService.filter(userId, status, method, date));
    }
}