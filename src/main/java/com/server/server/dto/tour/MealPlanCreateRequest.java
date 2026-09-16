package com.server.server.dto.tour;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.server.server.enums.tourmanagement.MealDietaryType;
import com.server.server.enums.tourmanagement.SpiceLevel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MealPlanCreateRequest {
    @NotBlank(message = "Meal name is required")
    private String mealName;

    @NotNull(message = "Meal price is required")
    @PositiveOrZero(message = "Meal price cannot be negative")
    private Double mealPrice;

    private String mealDescription;
    private Set<MealDietaryType> dietaryTypes;
    private SpiceLevel spiceLevel = SpiceLevel.NONE;
}
