package com.server.server.controllers.tourmanagement;

import java.util.List;

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

import com.server.server.dto.tour.ReservationResponse;
import com.server.server.dto.PageResponse;
import com.server.server.dto.filter.ReservationFilterRequest;
import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.services.tourmanagement.TourReservationService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
public class TourReservationController {
    private final TourReservationService reservationService;
    private final ModelMapper modelMapper;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public ResponseEntity<ApiResponse<ReservationResponse>> create(@Valid @RequestBody TourReservation res) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation created!", modelMapper.toReservationResponse(reservationService.create(res))));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ReservationResponse>> updateStatus(@NonNull @PathVariable Integer id, @RequestParam GenericStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toReservationResponse(reservationService.updateStatus(id, status))));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancelReservation(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok("Reservation cancelled!", modelMapper.toReservationResponse(reservationService.cancelReservation(id))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> search(
            @ModelAttribute ReservationFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(reservationService.filter(filter).map(modelMapper::toReservationResponse)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ReservationResponse>>> getAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.ok(reservationService.getAll(page, size, sortDir)
                .map(modelMapper::toReservationResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReservationResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toReservationResponse(reservationService.getById(id))));
    }
}
