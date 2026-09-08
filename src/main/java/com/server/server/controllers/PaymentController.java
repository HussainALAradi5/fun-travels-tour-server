package com.server.server.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.payment.PaymentResponse;
import com.server.server.dto.tour.ReservationResponse;
import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.enums.Payment.PaymentStatus;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.services.PaymentService;
import com.server.server.services.tourmanagement.TourReservationService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final TourReservationService reservationService;
    private final ModelMapper modelMapper;

    @PostMapping("/execute/{reservationId}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ReservationResponse>> executePayment(
            @PathVariable Integer reservationId,
            @RequestParam PaymentMethod method) {
        TourReservation result = reservationService.finalizeReservationWithPayment(reservationId, method);
        return ResponseEntity.ok(ApiResponse.ok("Payment processed!", modelMapper.toReservationResponse(result)));
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPayments(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) PaymentMethod method,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toPaymentResponseList(paymentService.filter(userId, status, method, date))));
    }
}
