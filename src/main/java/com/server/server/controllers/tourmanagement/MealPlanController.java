package com.server.server.controllers.tourmanagement;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
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

import com.server.server.dto.tour.MealPlanResponse;
import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.services.tourmanagement.MealPlanService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealPlanController {
    private final MealPlanService service;
    private final ModelMapper modelMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MealPlanResponse>>> getAllMeals() {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toMealPlanResponseList(service.findAll())));
    }

    @GetMapping("/agency/{agencyId}")
    public ResponseEntity<ApiResponse<List<MealPlanResponse>>> getMenu(@NonNull @PathVariable Integer agencyId) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toMealPlanResponseList(service.getAgencyCatalog(agencyId))));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<MealPlanResponse>> defineMeal(@Valid @RequestBody MealPlan mealPlan) {
        return ResponseEntity.ok(ApiResponse.ok("Meal added!", modelMapper.toMealPlanResponse(service.createMeal(mealPlan))));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> toggleStatus(@NonNull @PathVariable Integer id, @RequestParam GenericStatus status) {
        service.updateMealStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok("Status updated!"));
    }

    @PutMapping("/{id}/price")
    @PreAuthorize("hasAnyAuthority('OWNER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<MealPlanResponse>> updatePrice(@NonNull @PathVariable Integer id, @RequestParam Double price) {
        return ResponseEntity.ok(ApiResponse.ok("Price updated!", modelMapper.toMealPlanResponse(service.updatePricing(id, price))));
    }
}
