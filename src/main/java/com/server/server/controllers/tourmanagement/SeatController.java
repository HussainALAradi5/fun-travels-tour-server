package com.server.server.controllers.tourmanagement;

import com.server.server.dto.tourmanagement.SeatUpdateRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.tourmanagement.SeatResponse;
import com.server.server.dto.common.PageResponse;
import com.server.server.dto.tourmanagement.SeatFilterRequest;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.services.tourmanagement.SeatService;
import com.server.server.utilities.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {
    private final SeatService seatService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SeatResponse>>> getAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.ok(seatService.getAll(page, size, sortDir).map(SeatResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SeatResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(SeatResponse.from(seatService.getById(id))));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SeatResponse>> updateSeatDetails(@NonNull @PathVariable Integer id, @Valid @RequestBody SeatUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(SeatResponse.from(seatService.updateSeat(id, request))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<SeatResponse>>> search(@ModelAttribute SeatFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(seatService.search(filter)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SeatResponse>> updateStatus(@NonNull @PathVariable Integer id, @RequestParam SeatStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(SeatResponse.from(seatService.updateStatus(id, status))));
    }
}
