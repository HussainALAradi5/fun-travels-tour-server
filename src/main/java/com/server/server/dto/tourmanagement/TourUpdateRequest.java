package com.server.server.dto.tourmanagement;

import java.time.LocalDate;
import java.util.List;

import com.server.server.dto.common.EntityReference;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/** Partial tour update: null fields are left unchanged. References are {@code { "id": n }}. */
public record TourUpdateRequest(
        String title,
        String tourNumber,
        String description,
        @PositiveOrZero(message = "Base price cannot be negative") Double basePrice,
        LocalDate startDate,
        LocalDate endDate,
        @Positive(message = "Number of days must be positive") Integer numberOfDays,
        @Positive(message = "Capacity must be positive") Integer maxCapacity,
        EntityReference startCountry,
        EntityReference endCountry,
        EntityReference startCity,
        EntityReference endCity,
        EntityReference transportation,
        List<EntityReference> destinationCountries,
        List<EntityReference> availableMeals) {
}
