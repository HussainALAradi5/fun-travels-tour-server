package com.server.server.repositories.tourmanagement;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.server.server.enums.GenericStatus;
import com.server.server.models.tourmanagement.MealPlan;

@Repository
public interface MealPlanRepository extends JpaRepository<MealPlan, Integer> {
    List<MealPlan> findByAgencyIdAndStatus(Integer agencyId, GenericStatus status);
}
