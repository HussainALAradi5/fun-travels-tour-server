package com.server.server.services.tourmanagement;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.BeanUtils;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.repositories.tourmanagement.MealPlanRepository;
import com.server.server.dto.PageResponse;
import com.server.server.dto.tour.MealPlanCreateRequest;
import com.server.server.enums.tourmanagement.MealDietaryType;
import com.server.server.enums.tourmanagement.SpiceLevel;
import com.server.server.utilities.PaginationUtils;
import java.util.Set;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MealPlanService {
    private final MealPlanRepository repository;

    @Transactional(readOnly = true)
    public PageResponse<MealPlan> findAll(Integer page, Integer size, String sortDir) {
        return PageResponse.from(repository.findAll(PaginationUtils.pageable(page, size, "mealName", sortDir,
                "mealName", Set.of("mealName"))));
    }

    @Transactional(readOnly = true)
    public MealPlan findById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Meal definition not found: " + id));
    }

    @Transactional(readOnly = true)
    public PageResponse<MealPlan> getAgencyCatalog(@NonNull Integer agencyId, Integer page, Integer size) {
        Objects.requireNonNull(agencyId, "agencyId must not be null");
        return PageResponse.from(repository.findByAgencyIdAndStatus(agencyId, GenericStatus.ACTIVE,
                PaginationUtils.pageable(page, size, "mealName", "asc", "mealName", Set.of("mealName"))));
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public MealPlan createMeal(MealPlanCreateRequest request) {
        Set<MealDietaryType> types = request.getDietaryTypes() == null || request.getDietaryTypes().isEmpty()
                ? Set.of(MealDietaryType.STANDARD)
                : new java.util.HashSet<>(request.getDietaryTypes());
        if (types.contains(MealDietaryType.STANDARD) && types.size() > 1) {
            throw new IllegalArgumentException("Standard cannot be combined with another dietary classification.");
        }
        if (types.contains(MealDietaryType.VEGAN)) types.add(MealDietaryType.VEGETARIAN);

        MealPlan meal = new MealPlan();
        meal.setMealName(request.getMealName().trim());
        meal.setMealPrice(request.getMealPrice());
        meal.setMealDescription(request.getMealDescription());
        meal.setVegetarian(types.contains(MealDietaryType.VEGETARIAN));
        meal.setVegan(types.contains(MealDietaryType.VEGAN));
        meal.setGlutenFree(types.contains(MealDietaryType.GLUTEN_FREE));
        meal.setSpiceLevel(Objects.requireNonNullElse(request.getSpiceLevel(), SpiceLevel.NONE));
        meal.setStatus(GenericStatus.ACTIVE);
        return repository.save(meal);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public MealPlan updateMeal(@NonNull Integer id, MealPlan incomingData) {
        Objects.requireNonNull(id, "id must not be null");
        MealPlan existing = findById(id);
        if (incomingData.getMealPrice() != null && incomingData.getMealPrice() < 0) {
            throw new IllegalArgumentException("Meal price cannot be negative.");
        }
        
        BeanUtils.copyProperties(incomingData, existing, "id", "status", "agency");
        return repository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public void updateMealStatus(@NonNull Integer id, GenericStatus status) {
        Objects.requireNonNull(id, "id must not be null");
        MealPlan meal = findById(id);
        meal.setStatus(status);
        repository.save(meal);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public MealPlan updatePricing(@NonNull Integer id, Double newPrice) {
        Objects.requireNonNull(id, "id must not be null");
        if (newPrice == null || newPrice < 0) throw new IllegalArgumentException("Meal price cannot be negative or null.");
        MealPlan meal = findById(id);
        meal.setMealPrice(newPrice);
        return repository.save(meal);
    }
}
