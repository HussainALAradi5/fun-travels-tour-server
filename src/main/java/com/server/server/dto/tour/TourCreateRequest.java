package com.server.server.dto.tour;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TourCreateRequest {
    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    @NotNull(message = "Base price is required")
    private Double basePrice;
    private Double discountPrice;
    @NotNull(message = "Number of days is required")
    private Integer numberOfDays;
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    private LocalDate endDate;
    @NotNull(message = "Max capacity is required")
    private Integer maxCapacity;
    private Integer startCountryId;
    private Integer endCountryId;
    private Integer startCityId;
    private Integer endCityId;
    private List<Integer> destinationCountryIds;
    private Integer transportationId;
    private List<Integer> mealPlanIds;
}
