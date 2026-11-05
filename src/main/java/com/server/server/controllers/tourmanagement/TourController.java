package com.server.server.controllers.tourmanagement;

import com.server.server.dto.tourmanagement.TourUpdateRequest;

import java.time.LocalDate;

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

import com.server.server.dto.tourmanagement.TourResponse;
import com.server.server.dto.tourmanagement.TourCreateRequest;
import com.server.server.dto.common.PageResponse;
import com.server.server.dto.tourmanagement.TourFilterRequest;
import com.server.server.enums.common.GenericStatus;
import com.server.server.services.tourmanagement.TourService;
import com.server.server.services.tourmanagement.TourStatusService;
import com.server.server.services.tourmanagement.TourCatalogService;
import com.server.server.utilities.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/tours")
@RequiredArgsConstructor
public class TourController {
    private final TourService tourService;
    private final TourCatalogService tourCatalogService;
    private final TourStatusService tourStatusService;

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<PageResponse<TourResponse>>> getCatalog(
            @RequestParam(required = false) Integer startCountryId,
            @RequestParam(required = false) Integer endCountryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ResponseEntity.ok(ApiResponse.ok(tourCatalogService.getCatalogTours(startCountryId, endCountryId,
                startDate, endDate, page, size).map(TourResponse::from)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<TourResponse>>> search(@ModelAttribute TourFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(tourCatalogService.filter(filter).map(TourResponse::from)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TourResponse>>> getAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "startDate") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.ok(tourCatalogService.getAll(page, size, sortBy, sortDir)
                .map(TourResponse::from)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TourResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(TourResponse.from(tourCatalogService.getVisible(id))));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TourResponse>> create(@Valid @RequestBody TourCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tour created!", TourResponse.from(tourService.create(request))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TourResponse>> update(@NonNull @PathVariable Integer id, @Valid @RequestBody TourUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tour updated!", TourResponse.from(tourService.updateTour(id, request))));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TourResponse>> updateStatus(@NonNull @PathVariable Integer id, @RequestParam GenericStatus status) {
        return ResponseEntity.ok(ApiResponse.ok("Status updated!", TourResponse.from(tourStatusService.updateStatus(id, status))));
    }
}
