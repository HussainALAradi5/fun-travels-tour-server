package com.server.server.dto.tourmanagement;

import java.util.EnumSet;
import java.util.Set;

import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.tourmanagement.MealDietaryType;
import com.server.server.enums.tourmanagement.SpiceLevel;
import com.server.server.models.tourmanagement.MealPlan;
public record MealPlanResponse(
        Integer id,
        String mealName,
        Double mealPrice,
        String mealDescription,
        Set<MealDietaryType> dietaryTypes,
        SpiceLevel spiceLevel,
        GenericStatus status) {

    public static MealPlanResponse from(MealPlan meal) {
        if (meal == null) {
            return null;
        }
        return new MealPlanResponse(meal.getId(), meal.getMealName(), meal.getMealPrice(), meal.getMealDescription(),
                dietaryTypes(meal), meal.getSpiceLevel(), meal.getStatus());
    }

    /** The entity stores one flag per diet; a meal with none of them is STANDARD. */
    private static Set<MealDietaryType> dietaryTypes(MealPlan meal) {
        Set<MealDietaryType> types = EnumSet.noneOf(MealDietaryType.class);
        if (meal.isVegetarian()) {
            types.add(MealDietaryType.VEGETARIAN);
        }
        if (meal.isVegan()) {
            types.add(MealDietaryType.VEGAN);
        }
        if (meal.isGlutenFree()) {
            types.add(MealDietaryType.GLUTEN_FREE);
        }
        return types.isEmpty() ? EnumSet.of(MealDietaryType.STANDARD) : types;
    }
}
