package com.server.server.controllers.tourmanagement;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.tourmanagement.ReservationResponse;
import com.server.server.dto.common.PageResponse;
import com.server.server.dto.tourmanagement.ReservationFilterRequest;
import com.server.server.enums.common.GenericStatus;
import com.server.server.dto.tourmanagement.ReservationCreateRequest;
import com.server.server.services.tourmanagement.TourReservationService;
import com.server.server.utilities.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/reservations")
@RequiredArgsConstructor
public class TourReservationController {
    private final TourReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public ResponseEntity<ApiResponse<ReservationResponse>> create(@Valid @RequestBody ReservationCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation created!", ReservationResponse.from(reservationService.create(request))));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateStatus(@NonNull @PathVariable Integer id, @RequestParam GenericStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(ReservationResponse.from(reservationService.updateStatus(id, status))));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancelReservation(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation cancelled!", ReservationResponse.from(reservationService.cancelReservation(id))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> search(
            @ModelAttribute ReservationFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(reservationService.filter(filter).map(ReservationResponse::from)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> getAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.ok(reservationService.getAll(page, size, sortDir)
                .map(ReservationResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(ReservationResponse.from(reservationService.getById(id))));
    }
}
