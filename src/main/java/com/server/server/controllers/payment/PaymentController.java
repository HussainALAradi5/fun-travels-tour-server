package com.server.server.controllers.payment;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.payment.PaymentResponse;
import com.server.server.dto.common.PageResponse;
import com.server.server.dto.payment.PaymentFilterRequest;
import com.server.server.dto.tourmanagement.ReservationResponse;
import com.server.server.enums.payment.PaymentMethod;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.services.payment.PaymentService;
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
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<ReservationResponse>> executePayment(
            @NonNull @PathVariable Integer reservationId,
            @RequestParam PaymentMethod method) {
        TourReservation result = reservationService.finalizeReservationWithPayment(reservationId, method);
        return ResponseEntity.ok(ApiResponse.ok("Payment processed!", modelMapper.toReservationResponse(result)));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<PageResponse<PaymentResponse>>> getPayments(@ModelAttribute PaymentFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.filter(filter).map(modelMapper::toPaymentResponse)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'OWNER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toPaymentResponse(paymentService.getById(id))));
    }
}
