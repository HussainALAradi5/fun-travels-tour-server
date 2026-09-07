package com.server.server.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.models.City;
import com.server.server.services.CityService;
import com.server.server.utilities.ApiResponse;

@RestController
@RequestMapping("/api/cities")
@CrossOrigin(origins = "*")
public class CityController {

    @Autowired
    private CityService cityService;

    private void validateAdmin(String userType) {
        if (!"ADMIN".equalsIgnoreCase(userType)) {
            throw new RuntimeException("Access Denied: Only ADMINs can perform this action.");
        }
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<City>>> getAll(@RequestParam String userType) {
        validateAdmin(userType);
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched all cities", cityService.getAllCities()));
    }

    @GetMapping("/country/{countryId}")
    public ResponseEntity<ApiResponse<List<City>>> getByCountry(@PathVariable Integer countryId) {
        return ResponseEntity.ok(new ApiResponse<>(true, "Fetched cities", cityService.getCitiesByCountry(countryId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<City>> create(@RequestBody City city, @RequestParam String userType) {
        try {
            validateAdmin(userType);
            City savedCity = cityService.createCity(city);
            return ResponseEntity.ok(new ApiResponse<>(true, "City created successfully", savedCity));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Integer id, @RequestParam String userType) {
        try {
            validateAdmin(userType);
            cityService.deleteCity(id);
            return ResponseEntity.ok(new ApiResponse<>(true, "City deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}