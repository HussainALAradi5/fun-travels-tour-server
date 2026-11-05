package com.server.server.services.tourmanagement;

import com.server.server.models.tourmanagement.Transportation;
import com.server.server.models.geography.City;
import com.server.server.models.geography.Country;
import com.server.server.dto.common.EntityReference;
import com.server.server.dto.tourmanagement.TourUpdateRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.dto.tourmanagement.TourCreateRequest;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.exceptions.common.WorkflowException;
import com.server.server.models.user.User;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.repositories.tourmanagement.TourRepository;
import com.server.server.repositories.geography.CountryRepository;
import com.server.server.repositories.geography.CityRepository;
import com.server.server.repositories.tourmanagement.MealPlanRepository;
import com.server.server.services.support.GenericTrackingService;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.exceptions.common.AccessDeniedException;
import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.models.agency.Agency;
import com.server.server.repositories.tourmanagement.TicketRepository;
import com.server.server.exceptions.common.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;

/** Creating and editing tours, with their validation rules. */
@Service
@RequiredArgsConstructor
public class TourService {

    private final TourRepository tourRepository;
    private final TransportationService transportationService;
    private final AccessPolicyService accessPolicy;
    private final GenericTrackingService trackingService;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final MealPlanRepository mealPlanRepository;
    private final TicketRepository ticketRepository;

    @Transactional(readOnly = true)
    public Tour getById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return tourRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tour", id));
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public Tour create(TourCreateRequest request) {
        User currentUser = accessPolicy.currentUserOrNull();
        Tour tour = new Tour();
        tour.setTourNumber(request.tourNumber().trim());
        tour.setTitle(request.title().trim());
        tour.setDescription(request.description());
        tour.setBasePrice(request.basePrice());
        tour.setDiscountPrice(0.0);
        tour.setTotalPrice(request.basePrice());
        tour.setNumberOfDays(request.numberOfDays());
        tour.setStartDate(request.startDate());
        tour.setEndDate(request.endDate());
        tour.setMaxCapacity(request.maxCapacity());
        tour.setAvailableSlots(request.maxCapacity());
        tour.setStartCountry(countryRepository.findById(request.startCountryId())
                .orElseThrow(() -> new WorkflowException("Start country not found.")));
        tour.setEndCountry(countryRepository.findById(request.endCountryId())
                .orElseThrow(() -> new WorkflowException("End country not found.")));
        tour.setStartCity(cityRepository.findById(request.startCityId())
                .orElseThrow(() -> new WorkflowException("Start city not found.")));
        tour.setEndCity(cityRepository.findById(request.endCityId())
                .orElseThrow(() -> new WorkflowException("End city not found.")));
        if (!Objects.equals(tour.getStartCity().getCountry().getId(), tour.getStartCountry().getId())
                || !Objects.equals(tour.getEndCity().getCountry().getId(), tour.getEndCountry().getId())) {
            throw new WorkflowException("A selected city must belong to its selected country.");
        }
        if (request.destinationCountryIds() != null) {
            tour.getDestinationCountries().addAll(countryRepository.findAllById(request.destinationCountryIds()));
        }
        if (request.transportationId() != null) {
            tour.setTransportation(transportationService.getById(request.transportationId()));
            tour.setHasTransportation(true);
        }

        // Agency staff always create tours in their own agency (and branch, when they have one).
        if (AccessPolicyService.isAgencyStaff(currentUser)) {
            tour.setAgency(currentUser.getAgency());
            tour.setAgencyBranch(currentUser.getAgencyBranch());
        }

        if (tour.getTransportation() != null && tour.getTransportation().getId() != null) {
            tour.setTransportation(transportationService.getById(tour.getTransportation().getId()));
        }

        if (request.mealPlanIds() != null) {
            tour.getAvailableMeals().addAll(bookableMeals(request.mealPlanIds(), tour.getAgency()));
        }
        validateTourDates(tour.getStartDate(), tour.getEndDate());
        validateCapacity(tour);
        validateTransportationAvailability(tour, null);
        tour.setTotalPrice(tour.getBasePrice());
        tour.setStatus(GenericStatus.PENDING);

        Tour savedTour = tourRepository.save(tour);

        // EVENT LOG: Tour Created
        trackingService.logEvent(
                savedTour.getId(),
                ReferenceType.TOUR,
                "CREATED",
                "Tour was created.",
                currentUser);

        return savedTour;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public Tour updateTour(@NonNull Integer id, TourUpdateRequest request) {
        Tour existing = getById(id);
        User currentUser = accessPolicy.requireCurrentUser();
        requireCanManage(currentUser, existing);
        if (existing.getStatus() == GenericStatus.COMPLETED) {
            throw new WorkflowException("Cannot modify a completed tour.");
        }
        if (request.title() != null && !request.title().isBlank()) existing.setTitle(request.title().trim());
        if (request.tourNumber() != null && !request.tourNumber().isBlank()) existing.setTourNumber(request.tourNumber().trim());
        if (request.description() != null) existing.setDescription(request.description());
        if (request.numberOfDays() != null) existing.setNumberOfDays(request.numberOfDays());
        boolean datesChanged = (request.startDate() != null && !request.startDate().equals(existing.getStartDate()))
                || (request.endDate() != null && !request.endDate().equals(existing.getEndDate()));
        if (request.startDate() != null) existing.setStartDate(request.startDate());
        if (request.endDate() != null) existing.setEndDate(request.endDate());
        if (datesChanged) {
            // An already-running tour keeps its past start date; only new dates are checked.
            validateTourDates(existing.getStartDate(), existing.getEndDate());
        }

        if (request.maxCapacity() != null) {
            int bookedOrHeld = existing.getMaxCapacity() - existing.getAvailableSlots();
            if (request.maxCapacity() < bookedOrHeld) {
                throw new WorkflowException("CAPACITY_BELOW_BOOKINGS",
                        "Capacity cannot be lower than the number of reserved or sold places.");
            }
            existing.setMaxCapacity(request.maxCapacity());
            existing.setAvailableSlots(request.maxCapacity() - bookedOrHeld);
        }
        if (request.basePrice() != null) {
            existing.setBasePrice(request.basePrice());
        }

        Country startCountry = findCountry(request.startCountry());
        Country endCountry = findCountry(request.endCountry());
        City startCity = findCity(request.startCity());
        City endCity = findCity(request.endCity());
        if (startCountry != null) existing.setStartCountry(startCountry);
        if (endCountry != null) existing.setEndCountry(endCountry);
        if (startCity != null) existing.setStartCity(startCity);
        if (endCity != null) existing.setEndCity(endCity);
        if (!Objects.equals(existing.getStartCity().getCountry().getId(), existing.getStartCountry().getId())
                || !Objects.equals(existing.getEndCity().getCountry().getId(), existing.getEndCountry().getId())) {
            throw new WorkflowException("A selected city must belong to its selected country.");
        }

        Integer transportationId = idOf(request.transportation());
        if (transportationId != null && (existing.getTransportation() == null
                || !Objects.equals(existing.getTransportation().getId(), transportationId))) {
            if (ticketRepository.existsByTour_IdAndTicketStatusNot(existing.getId(), TicketStatus.CANCELLED)) {
                throw new WorkflowException("TRANSPORTATION_LOCKED",
                        "Transportation cannot change after tickets were issued for this tour.");
            }
            Transportation transportation = transportationService.getById(transportationId);
            accessPolicy.requireAgencyAccess(currentUser, transportation.getAgency());
            existing.setTransportation(transportation);
            existing.setHasTransportation(true);
        }
        validateCapacity(existing);
        validateTransportationAvailability(existing, existing.getId());

        if (request.destinationCountries() != null) {
            existing.getDestinationCountries().clear();
            existing.getDestinationCountries().addAll(countryRepository.findAllById(ids(request.destinationCountries())));
        }
        if (request.availableMeals() != null) {
            existing.getAvailableMeals().clear();
            existing.getAvailableMeals().addAll(bookableMeals(ids(request.availableMeals()), existing.getAgency()));
        }

        double discount = existing.getDiscountPrice() != null ? existing.getDiscountPrice() : 0.0;
        existing.setTotalPrice(existing.getBasePrice() - discount);

        Tour updatedTour = tourRepository.save(existing);

        // EVENT LOG: Tour Updated
        trackingService.logEvent(
                updatedTour.getId(),
                ReferenceType.TOUR,
                "UPDATED",
                "Tour details were modified.",
                currentUser);

        return updatedTour;
    }

    private void validateTourDates(LocalDate start, LocalDate end) {
        if (start == null) {
            throw new WorkflowException("Start date is required.");
        }
        if (start.isBefore(LocalDate.now())) {
            throw new WorkflowException("Start date cannot be in the past.");
        }
        if (end != null && end.isBefore(start)) {
            throw new WorkflowException("End date cannot be earlier than the start date.");
        }
    }

    /**
     * Admins manage every tour. Agency staff manage tours of their own agency: owners all of them,
     * managers those of their branch (or without a branch), employees only tours they created.
     */
    private static Integer idOf(EntityReference reference) {
        return reference == null ? null : reference.id();
    }

    private static List<Integer> ids(List<EntityReference> references) {
        return references.stream().map(TourService::idOf).filter(Objects::nonNull).distinct().toList();
    }

    private Country findCountry(EntityReference reference) {
        Integer id = idOf(reference);
        return id == null ? null : countryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Country", id));
    }

    private City findCity(EntityReference reference) {
        Integer id = idOf(reference);
        return id == null ? null : cityRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("City", id));
    }

    /** Meals offered on a tour must be active and belong to the tour's agency (or be shared). */
    private List<MealPlan> bookableMeals(List<Integer> mealIds, Agency agency) {
        List<MealPlan> meals = mealPlanRepository.findAllById(mealIds);
        if (meals.size() != new java.util.HashSet<>(mealIds).size()) {
            throw new ResourceNotFoundException("One or more meal plans were not found.");
        }
        for (MealPlan meal : meals) {
            if (meal.getStatus() != GenericStatus.ACTIVE) {
                throw new WorkflowException("MEAL_INACTIVE", "Meal '" + meal.getMealName() + "' is not active.");
            }
            if (meal.getAgency() != null && (agency == null || !Objects.equals(meal.getAgency().getId(), agency.getId()))) {
                throw new WorkflowException("MEAL_OTHER_AGENCY", "Meal '" + meal.getMealName() + "' belongs to another agency.");
            }
        }
        return meals;
    }

    /** Used by the status workflow too. */
    void requireCanManage(User user, Tour tour) {
        if (AccessPolicyService.isAdmin(user)) {
            return;
        }
        accessPolicy.requireAgencyAccess(user, tour.getAgency());
        boolean creator = tour.getCreatedBy() != null && Objects.equals(tour.getCreatedBy().getId(), user.getId());
        boolean allowed = switch (user.getUserType()) {
            case OWNER -> true;
            case MANAGER -> creator || tour.getAgencyBranch() == null || (user.getAgencyBranch() != null
                    && Objects.equals(tour.getAgencyBranch().getId(), user.getAgencyBranch().getId()));
            case EMPLOYEE -> creator;
            default -> false;
        };
        if (!allowed) {
            throw new AccessDeniedException(
                    "You can only manage tours you created, or tours within your managed branch or agency.");
        }
    }

    private void validateCapacity(Tour tour) {
        if (tour.getMaxCapacity() == null || tour.getMaxCapacity() <= 0) {
            throw new WorkflowException("Tour capacity must be greater than zero.");
        }
        if (tour.getTransportation() != null && tour.getTransportation().getTotalCapacity() != null
                && tour.getMaxCapacity() > tour.getTransportation().getTotalCapacity()) {
            throw new WorkflowException("Tour capacity cannot exceed transportation capacity.");
        }
        if (tour.getBasePrice() != null && tour.getDiscountPrice() != null
                && tour.getDiscountPrice() > tour.getBasePrice()) {
            throw new WorkflowException("Discount cannot exceed the base price.");
        }
    }

    private void validateTransportationAvailability(Tour tour, Integer excludedTourId) {
        if (tour.getTransportation() == null || tour.getTransportation().getId() == null) return;
        LocalDate end = tour.getEndDate() == null ? tour.getStartDate() : tour.getEndDate();
        if (tourRepository.hasTransportationConflict(tour.getTransportation().getId(), excludedTourId,
                tour.getStartDate(), end)) {
            throw new WorkflowException("Transportation is already assigned to an overlapping tour.");
        }
    }
}
