package com.server.server.controllers.tourmanagement;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.enums.GenericStatus;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.services.tourmanagement.TourService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tours")
@RequiredArgsConstructor
public class TourController {
    private final TourService tourService;

    @GetMapping("/catalog")
    public ResponseEntity<List<Tour>> getCatalog(
            @RequestParam(required = false) Integer startCountryId,
            @RequestParam(required = false) Integer endCountryId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return ResponseEntity.ok(tourService.getCatalogTours(startCountryId, endCountryId, startDate, endDate));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<Tour>> filter(
            @RequestParam(required = false) GenericStatus status,
            @RequestParam(required = false) Integer minSlots,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long agencyId,
            @RequestParam(required = false) Long branchId, 
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Integer countryId,
            @RequestParam(required = false) Integer cityId,
            @RequestParam(required = false) Integer createdById,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDir) {

        return ResponseEntity.ok(tourService.filter(
                status, minSlots, startDate, endDate, agencyId, branchId, 
                minPrice, maxPrice, countryId, cityId, createdById,
                sortBy, sortDir));
    }

    @GetMapping
    public ResponseEntity<List<Tour>> getAll() {
        return ResponseEntity.ok(tourService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Tour> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(tourService.getById(id));
    }

    @PostMapping
    public ResponseEntity<Tour> create(@RequestBody Tour tour) {
        return ResponseEntity.ok(tourService.create(tour));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Tour> update(@PathVariable Integer id, @RequestBody Tour tour) {
        return ResponseEntity.ok(tourService.updateTour(id, tour));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Integer id, @RequestParam GenericStatus status) {
        try {
            return ResponseEntity.ok(tourService.updateStatus(id, status));
        } catch (WorkflowException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }
}