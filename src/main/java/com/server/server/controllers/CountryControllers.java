package com.server.server.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.server.server.models.Country;
import com.server.server.services.CountryService;

@RestController
@RequestMapping("/api/countries")
@CrossOrigin(origins = "*")
public class CountryControllers {

    @Autowired
    private CountryService countryService;

    @GetMapping
    public ResponseEntity<List<Country>> getAll() {
        return ResponseEntity.ok(countryService.getAllCountries());
    }

    @PostMapping("/sync/{name}")
    public ResponseEntity<Map<String, Object>> syncExternal(
            @PathVariable String name,
            @RequestParam String userType) {
        Map<String, Object> response = new HashMap<>();
        try {
            Country saved = countryService.syncFromExternal(name, userType);
            response.put("success", true);
            response.put("data", saved);
            response.put("message", "Country synced and saved successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addManual(
            @RequestBody Country country,
            @RequestParam String userType) {

        Map<String, Object> response = new HashMap<>();
        try {
            Country saved = countryService.createCountry(country, userType);
            response.put("success", true);
            response.put("data", saved);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/sync-all")
    public ResponseEntity<Map<String, Object>> syncAll(@RequestParam String userType) {
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Integer> stats = countryService.syncAllCountries(userType);
            response.put("success", true);
            response.put("added", stats.get("added"));
            response.put("skipped", stats.get("skipped"));
            response.put("message", "Bulk sync completed successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    } // Braces fixed here

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteCountry(
            @PathVariable Integer id,
            @RequestParam String userType) {
        Map<String, Object> response = new HashMap<>();
        try {
            countryService.deleteCountry(id, userType);
            response.put("success", true);
            response.put("message", "Country deleted successfully!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}