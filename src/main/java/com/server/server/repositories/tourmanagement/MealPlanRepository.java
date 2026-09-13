package com.server.server.repositories.tourmanagement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.MealPlan;

public interface MealPlanRepository extends JpaRepository<MealPlan, Integer> {
    List<MealPlan> findByAgencyIdAndStatus(Integer agencyId, GenericStatus status);
    Page<MealPlan> findByAgencyIdAndStatus(Integer agencyId, GenericStatus status, Pageable pageable);
}
