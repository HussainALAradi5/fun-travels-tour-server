package com.server.server.dto.tourmanagement;

import java.time.LocalDate;
import java.util.List;

import com.server.server.dto.common.EntityReference;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/** Partial tour update: null fields are left unchanged. References are {@code { "id": n }}. */
@Data
public class TourUpdateRequest {
    private String title;
    private String tourNumber;
    private String description;
    @PositiveOrZero(message = "Base price cannot be negative")
    private Double basePrice;
    private LocalDate startDate;
    private LocalDate endDate;
    @Positive(message = "Number of days must be positive")
    private Integer numberOfDays;
    @Positive(message = "Capacity must be positive")
    private Integer maxCapacity;
    private EntityReference startCountry;
    private EntityReference endCountry;
    private EntityReference startCity;
    private EntityReference endCity;
    private EntityReference transportation;
    private List<EntityReference> destinationCountries;
    private List<EntityReference> availableMeals;
}
