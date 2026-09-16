package com.server.server.dto.tour;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.MealDietaryType;
import com.server.server.enums.tourmanagement.SpiceLevel;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MealPlanResponse {
    private Integer id;
    private String mealName;
    private Double mealPrice;
    private String mealDescription;
    private Set<MealDietaryType> dietaryTypes;
    private SpiceLevel spiceLevel;
    private GenericStatus status;
}
