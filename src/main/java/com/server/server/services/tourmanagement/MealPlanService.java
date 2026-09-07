package com.server.server.services.tourmanagement;

import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.repositories.tourmanagement.MealPlanRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MealPlanService {
    private final MealPlanRepository repository;

    @Transactional(readOnly = true)
    public List<MealPlan> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public MealPlan findById(Integer id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Meal definition not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<MealPlan> getAgencyCatalog(Integer agencyId) {
        return repository.findByAgencyIdAndStatus(agencyId, GenericStatus.ACTIVE);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public MealPlan createMeal(MealPlan meal) {
        if (meal.getMealPrice() < 0) throw new IllegalArgumentException("Meal price cannot be negative.");
        meal.setStatus(GenericStatus.ACTIVE);
        return repository.save(meal);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public MealPlan updateMeal(Integer id, MealPlan incomingData) {
        MealPlan existing = findById(id);
        if (incomingData.getMealPrice() != null && incomingData.getMealPrice() < 0) {
            throw new IllegalArgumentException("Meal price cannot be negative.");
        }
        
        BeanUtils.copyProperties(incomingData, existing, "id", "status", "agency");
        return repository.save(existing);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER')")
    public void updateMealStatus(Integer id, GenericStatus status) {
        MealPlan meal = findById(id);
        meal.setStatus(status);
        repository.save(meal);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public MealPlan updatePricing(Integer id, Double newPrice) {
        if (newPrice == null || newPrice < 0) throw new IllegalArgumentException("Meal price cannot be negative or null.");
        MealPlan meal = findById(id);
        meal.setMealPrice(newPrice);
        return repository.save(meal);
    }
}