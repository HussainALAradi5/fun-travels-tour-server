package com.server.server.dto.tourmanagement;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
public record TourCreateRequest(
        @NotBlank(message = "Tour code is required") String tourNumber,
        @NotBlank(message = "Title is required") String title,
        String description,
        @NotNull(message = "Base price is required") @PositiveOrZero(message = "Base price cannot be negative") Double basePrice,
        /** Optional sale amount taken off the base price for every traveler. */
        @PositiveOrZero(message = "Discount cannot be negative") Double discountPrice,
        @NotNull(message = "Number of days is required") Integer numberOfDays,
        @NotNull(message = "Start date is required") LocalDate startDate,
        LocalDate endDate,
        @NotNull(message = "Max capacity is required") Integer maxCapacity,
        @NotNull(message = "Start country is required") Integer startCountryId,
        @NotNull(message = "End country is required") Integer endCountryId,
        @NotNull(message = "Start city is required") Integer startCityId,
        @NotNull(message = "End city is required") Integer endCityId,
        List<Integer> destinationCountryIds,
        Integer transportationId,
        List<Integer> mealPlanIds) {
}
