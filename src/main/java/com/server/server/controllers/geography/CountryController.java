package com.server.server.controllers.geography;

import jakarta.validation.Valid;
import com.server.server.dto.geography.CountryCreateRequest;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.geography.CountryResponse;
import com.server.server.models.geography.Country;
import com.server.server.services.geography.CountryService;
import com.server.server.utilities.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/countries")
@RequiredArgsConstructor
public class CountryController {

    private final CountryService countryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CountryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(countryService.getAllCountries().stream().map(CountryResponse::from).toList()));
    }

    @PostMapping("/sync/{name}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CountryResponse>> syncExternal(@PathVariable String name) {
        Country saved = countryService.syncFromExternal(name);
        return ResponseEntity.ok(ApiResponse.ok("Country synced successfully!", CountryResponse.from(saved)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<CountryResponse>> addManual(@Valid @RequestBody CountryCreateRequest request) {
        Country saved = countryService.createCountry(request);
        return ResponseEntity.ok(ApiResponse.ok("Country created successfully!", CountryResponse.from(saved)));
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
