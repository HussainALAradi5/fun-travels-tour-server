package com.server.server.controllers.tourmanagement;

import java.util.List;

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

import com.server.server.dto.tour.SeatResponse;
import com.server.server.dto.filter.SeatFilterRequest;
import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.services.tourmanagement.SeatService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/seats")
@RequiredArgsConstructor
public class SeatController {
    private final SeatService seatService;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toSeatResponseList(seatService.getAll())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SeatResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toSeatResponse(seatService.getById(id))));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SeatResponse>> updateSeatDetails(@NonNull @PathVariable Integer id, @Valid @RequestBody Seat seat) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toSeatResponse(seatService.updateSeat(id, seat))));
    }

    @GetMapping("/filter")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> filter(@ModelAttribute SeatFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toSeatResponseList(seatService.filter(filter))));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<SeatResponse>> updateStatus(@NonNull @PathVariable Integer id, @RequestParam SeatStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toSeatResponse(seatService.updateStatus(id, status))));
    }
}
