package com.server.server.controllers.tourmanagement;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.services.tourmanagement.MealPlanService;
import com.server.server.utilities.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealPlanController {
    private final MealPlanService service;

    @GetMapping
    public ResponseEntity<?> getAllMeals() {
        try {
            return ResponseEntity.ok(new ApiResponse<>(true, "All meals retrieved", service.findAll()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @GetMapping("/agency/{agencyId}")
    public ResponseEntity<?> getMenu(@PathVariable Integer agencyId) {
        try {
            return ResponseEntity.ok(new ApiResponse<>(true, "Meal catalog retrieved", service.getAgencyCatalog(agencyId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> defineMeal(@Valid @RequestBody MealPlan mealPlan) {
        try {
            return ResponseEntity.ok(new ApiResponse<>(true, "New meal added to catalog", service.createMeal(mealPlan)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> toggleStatus(@PathVariable Integer id, @RequestParam GenericStatus status) {
        try {
            service.updateMealStatus(id, status);
            return ResponseEntity.ok(new ApiResponse<>(true, "Meal status updated to " + status, null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    @PutMapping("/{id}/price")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<?> updatePrice(@PathVariable Integer id, @RequestParam Double price) {
        try {
            return ResponseEntity.ok(new ApiResponse<>(true, "Price updated", service.updatePricing(id, price)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}