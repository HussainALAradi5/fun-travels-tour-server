package com.server.server.dto.tourmanagement;

import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.server.server.enums.tourmanagement.MealDietaryType;
import com.server.server.enums.tourmanagement.SpiceLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A meal package; the spice level defaults to NONE.
 * @param agencyId Admins only; agency staff always create meals for their own agency.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record MealPlanCreateRequest(
        @NotBlank(message = "Meal name is required") String mealName,
        @NotNull(message = "Meal price is required") @PositiveOrZero(message = "Meal price cannot be negative") Double mealPrice,
        String mealDescription,
        Set<MealDietaryType> dietaryTypes,
        SpiceLevel spiceLevel,
        Integer agencyId) {

    public MealPlanCreateRequest {
        spiceLevel = Objects.requireNonNullElse(spiceLevel, SpiceLevel.NONE);
    }
}
