package com.server.server.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.geography.CountryResponse;
import com.server.server.models.Country;
import com.server.server.services.CountryService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/countries")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CountryControllers {

    private final CountryService countryService;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toCountryResponseList(countryService.getAllCountries())));
    }

    @PostMapping("/sync/{name}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CountryResponse>> syncExternal(@PathVariable String name) {
        Country saved = countryService.syncFromExternal(name);
        return ResponseEntity.ok(ApiResponse.ok("Country synced successfully!", modelMapper.toCountryResponse(saved)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CountryResponse>> addManual(@RequestBody Country country) {
        Country saved = countryService.createCountry(country);
        return ResponseEntity.ok(ApiResponse.ok("Country created successfully!", modelMapper.toCountryResponse(saved)));
    }

    @PostMapping("/sync-all")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> syncAll() {
        Map<String, Integer> stats = countryService.syncAllCountries();
        return ResponseEntity.ok(ApiResponse.ok("Bulk sync completed!", stats));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCountry(@PathVariable @org.springframework.lang.NonNull Integer id) {
        countryService.deleteCountry(id);
        return ResponseEntity.ok(ApiResponse.ok("Country deleted successfully!"));
    }
}
