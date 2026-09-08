package com.server.server.dto.tour;

import com.server.server.enums.GenericStatus;

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
    private boolean isVegetarian;
    private boolean isVegan;
    private boolean isGlutenFree;
    private GenericStatus status;
}
