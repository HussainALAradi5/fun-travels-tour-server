package com.server.server.controllers.geography;

import jakarta.validation.Valid;

import com.server.server.dto.geography.CityCreateRequest;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.geography.CityResponse;
import com.server.server.models.geography.City;
import com.server.server.services.geography.CityService;
import com.server.server.utilities.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/cities")
@RequiredArgsConstructor
public class CityController {

    private final CityService cityService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CityResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(cityService.getAllCities().stream().map(CityResponse::from).toList()));
    }

    @GetMapping("/country/{countryId}")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getByCountry(@NonNull @PathVariable Integer countryId) {
        return ResponseEntity.ok(ApiResponse.ok(cityService.getCitiesByCountry(countryId).stream().map(CityResponse::from).toList()));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CityResponse>> create(@Valid @RequestBody CityCreateRequest request) {
        City savedCity = cityService.createCity(request);
        return ResponseEntity.ok(ApiResponse.ok("City created successfully!", CityResponse.from(savedCity)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@NonNull @PathVariable Integer id) {
        cityService.deleteCity(id);
        return ResponseEntity.ok(ApiResponse.ok("City deleted successfully!"));
    }
}
