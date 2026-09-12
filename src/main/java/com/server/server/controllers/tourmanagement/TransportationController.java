package com.server.server.controllers.tourmanagement;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.tour.TransportationResponse;
import com.server.server.dto.PageResponse;
import com.server.server.dto.filter.TransportationFilterRequest;
import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.TransportationStatus;
import com.server.server.enums.tourmanagement.TransportationType;
import com.server.server.models.tourmanagement.Transportation;
import com.server.server.services.tourmanagement.TransportationService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transportations")
@RequiredArgsConstructor
public class TransportationController {
    private final TransportationService service;
    private final ModelMapper modelMapper;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransportationResponse>> create(@Valid @RequestBody Transportation transportation) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transportation created!", modelMapper.toTransportationResponse(service.create(transportation))));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransportationResponse>>> getAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "transportationNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll(page, size, sortBy, sortDir)
                .map(modelMapper::toTransportationResponse)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransportationResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toTransportationResponse(service.getById(id))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransportationResponse>> update(@NonNull @PathVariable Integer id, @Valid @RequestBody Transportation transportation) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toTransportationResponse(service.update(id, transportation))));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransportationResponse>> updateStatus(@NonNull @PathVariable Integer id, @RequestParam TransportationStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toTransportationResponse(service.updateStatus(id, status))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<TransportationResponse>>> search(
            @ModelAttribute TransportationFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(service.filter(filter).map(modelMapper::toTransportationResponse)));
    }
}
