package com.server.server.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.geography.CityResponse;
import com.server.server.models.City;
import com.server.server.services.CityService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CityController {

    private final CityService cityService;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CityResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toCityResponseList(cityService.getAllCities())));
    }

    @GetMapping("/country/{countryId}")
    public ResponseEntity<ApiResponse<List<CityResponse>>> getByCountry(@NonNull @PathVariable Integer countryId) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toCityResponseList(cityService.getCitiesByCountry(countryId))));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CityResponse>> create(@RequestBody City city) {
        City savedCity = cityService.createCity(city);
        return ResponseEntity.ok(ApiResponse.ok("City created successfully!", modelMapper.toCityResponse(savedCity)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@NonNull @PathVariable Integer id) {
        cityService.deleteCity(id);
        return ResponseEntity.ok(ApiResponse.ok("City deleted successfully!"));
    }
}
