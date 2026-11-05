package com.server.server.controllers.tourmanagement;

import com.server.server.dto.tourmanagement.TransportationUpdateRequest;

import java.io.IOException;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.server.server.dto.tourmanagement.TransportationResponse;
import com.server.server.dto.common.PageResponse;
import com.server.server.dto.tourmanagement.TransportationFilterRequest;
import com.server.server.dto.common.ImportResult;
import com.server.server.dto.tourmanagement.TransportationCreateRequest;
import com.server.server.enums.tourmanagement.TransportationStatus;
import com.server.server.services.tourmanagement.TransportationImportService;
import com.server.server.services.tourmanagement.TransportationService;
import com.server.server.utilities.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transportations")
@RequiredArgsConstructor
public class TransportationController {
    private final TransportationService service;
    private final TransportationImportService importService;

    @PostMapping
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransportationResponse>> create(@Valid @RequestBody TransportationCreateRequest transportation) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Transportation created!", TransportationResponse.from(service.create(transportation))));
    }

    @PostMapping(value = "/imports", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<ImportResult>> importExcel(@RequestPart("file") MultipartFile file)
            throws IOException {
        if (file.isEmpty()) throw new IllegalArgumentException("Select a non-empty Excel file.");
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase().endsWith(".xlsx")
                && !filename.toLowerCase().endsWith(".xls"))) {
            throw new IllegalArgumentException("Only .xlsx and .xls files are supported.");
        }
        ImportResult result = importService.importExcel(file.getInputStream());
        return ResponseEntity.ok(ApiResponse.ok(
                result.failedCount() == 0 ? "Transportation import completed." : "Import completed with row errors.",
                result));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TransportationResponse>>> getAll(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(defaultValue = "transportationNumber") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll(page, size, sortBy, sortDir)
                .map(TransportationResponse::listItem)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TransportationResponse>> getById(@NonNull @PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(TransportationResponse.from(service.getVisible(id))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransportationResponse>> update(@NonNull @PathVariable Integer id, @Valid @RequestBody TransportationUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(TransportationResponse.from(service.update(id, request))));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<TransportationResponse>> updateStatus(@NonNull @PathVariable Integer id, @RequestParam TransportationStatus status) {
        return ResponseEntity.ok(ApiResponse.ok(TransportationResponse.from(service.updateStatus(id, status))));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<TransportationResponse>>> search(
            @ModelAttribute TransportationFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(service.filter(filter).map(TransportationResponse::listItem)));
    }
}
