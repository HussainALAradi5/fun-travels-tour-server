package com.server.server.controllers.tourmanagement;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.tour.TourResponse;
import com.server.server.dto.PageResponse;
import com.server.server.dto.filter.TourFilterRequest;
import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.services.tourmanagement.TourService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourController {
    private final TourService tourService;
    private final ModelMapper modelMapper;

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<PageResponse<TourResponse>>> getCatalog(
            @RequestParam(required = false) Integer startCountryId,
            @RequestParam(required = false) Integer endCountryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(ApiResponse.ok(tourService.getCatalogTours(startCountryId, endCountryId,
                startDate, endDate, page, size).map(modelMapper::toTourResponse)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<TourResponse>>> search(@ModelAttribute TourFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(tourService.filter(filter).map(modelMapper::toTourResponse)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TourResponse>>> getAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.ok(tourService.getAll(page, size, sortBy, sortDir)
                .map(modelMapper::toTourResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TourResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toTourResponse(tourService.getById(id))));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TourResponse>> create(@Valid @RequestBody Tour tour) {
        return ResponseEntity.ok(ApiResponse.ok("Tour created!", modelMapper.toTourResponse(tourService.create(tour))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TourResponse>> update(@NonNull @PathVariable Integer id, @Valid @RequestBody Tour tour) {
        return ResponseEntity.ok(ApiResponse.ok("Tour updated!", modelMapper.toTourResponse(tourService.updateTour(id, tour))));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TourResponse>> updateStatus(@NonNull @PathVariable Integer id, @RequestParam GenericStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated!", modelMapper.toTourResponse(tourService.updateStatus(id, status))));
    }
}
