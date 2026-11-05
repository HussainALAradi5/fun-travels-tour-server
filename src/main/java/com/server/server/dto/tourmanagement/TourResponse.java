package com.server.server.dto.tourmanagement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.server.server.dto.agency.AgencyPolicyResponse;
import com.server.server.dto.agency.AgencySummary;
import com.server.server.dto.agency.BranchSummary;
import com.server.server.dto.geography.CitySummary;
import com.server.server.dto.geography.CountrySummary;
import com.server.server.dto.user.UserSummary;
import com.server.server.enums.common.GenericStatus;
import com.server.server.models.tourmanagement.Tour;
/** @param availableMeals active meals travelers may add to their tickets */
public record TourResponse(
        Integer id,
        String tourNumber,
        String title,
        String description,
        Double basePrice,
        Double discountPrice,
        Double totalPrice,
        Integer numberOfDays,
        LocalDate startDate,
        LocalDate endDate,
        Integer maxCapacity,
        Integer availableSlots,
        GenericStatus status,
        boolean hasTransportation,
        CountrySummary startCountry,
        CountrySummary endCountry,
        List<CountrySummary> destinationCountries,
        CitySummary startCity,
        CitySummary endCity,
        AgencySummary agency,
        BranchSummary agencyBranch,
        TransportationSummary transportation,
        UserSummary createdBy,
        LocalDateTime createdAt,
        List<MealPlanResponse> availableMeals,
        AgencyPolicyResponse policy) {

    public static TourResponse from(Tour tour) {
        if (tour == null) {
            return null;
        }
        List<CountrySummary> destinations = tour.getDestinationCountries() == null ? List.of()
                : tour.getDestinationCountries().stream().map(CountrySummary::from).toList();
        List<MealPlanResponse> meals = tour.getAvailableMeals() == null ? List.of()
                : tour.getAvailableMeals().stream()
                        .filter(meal -> meal.getStatus() == GenericStatus.ACTIVE)
                        .map(MealPlanResponse::from).toList();
        return new TourResponse(tour.getId(), tour.getTourNumber(), tour.getTitle(), tour.getDescription(),
                tour.getBasePrice(), tour.getDiscountPrice(), tour.getTotalPrice(), tour.getNumberOfDays(),
                tour.getStartDate(), tour.getEndDate(), tour.getMaxCapacity(), tour.getAvailableSlots(), tour.getStatus(),
                tour.isHasTransportation(), CountrySummary.from(tour.getStartCountry()),
                CountrySummary.from(tour.getEndCountry()), destinations, CitySummary.from(tour.getStartCity()),
                CitySummary.from(tour.getEndCity()), AgencySummary.from(tour.getAgency()),
                BranchSummary.from(tour.getAgencyBranch()), TransportationSummary.from(tour.getTransportation()),
                UserSummary.from(tour.getCreatedBy()), tour.getCreatedAt(), meals,
                tour.getAgency() == null ? null : AgencyPolicyResponse.from(tour.getAgency()));
    }
}
