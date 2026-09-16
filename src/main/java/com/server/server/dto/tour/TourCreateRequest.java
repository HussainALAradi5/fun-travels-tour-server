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
    @NotBlank(message = "Tour code is required")
    private String tourNumber;
    @NotBlank(message = "Title is required")
    private String title;
    private String description;
    @NotNull(message = "Base price is required")
    private Double basePrice;
    @NotNull(message = "Number of days is required")
    private Integer numberOfDays;
    @NotNull(message = "Start date is required")
    private LocalDate startDate;
    private LocalDate endDate;
    @NotNull(message = "Max capacity is required")
    private Integer maxCapacity;
    @NotNull(message = "Start country is required")
    private Integer startCountryId;
    @NotNull(message = "End country is required")
    private Integer endCountryId;
    @NotNull(message = "Start city is required")
    private Integer startCityId;
    @NotNull(message = "End city is required")
    private Integer endCityId;
    private List<Integer> destinationCountryIds;
    private Integer transportationId;
    private List<Integer> mealPlanIds;
}
