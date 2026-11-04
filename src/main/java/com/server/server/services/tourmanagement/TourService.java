package com.server.server.services.tourmanagement;

import com.server.server.models.tourmanagement.Transportation;

import com.server.server.models.geography.City;

import com.server.server.models.geography.Country;

import com.server.server.dto.common.EntityReference;

import com.server.server.dto.tourmanagement.TourUpdateRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.payment.TransactionType;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.dto.tourmanagement.TourFilterRequest;
import com.server.server.dto.tourmanagement.TourCreateRequest;
import com.server.server.dto.common.PageResponse;
import com.server.server.services.common.GenericFilterService;
import java.util.Set;
import java.util.Map;
import com.server.server.enums.notification.NotificationType;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.enums.user.UserTypeEnum;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.user.User;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.repositories.tourmanagement.TourRepository;
import com.server.server.repositories.tourmanagement.TourReservationRepository;
import com.server.server.repositories.geography.CountryRepository;
import com.server.server.repositories.geography.CityRepository;
import com.server.server.repositories.tourmanagement.MealPlanRepository;
import com.server.server.services.support.GenericTrackingService;
import com.server.server.services.notification.NotificationService;
import com.server.server.services.common.SystemSchedulingService;
import com.server.server.utilities.DomainWorkflowValidator;
import com.server.server.utilities.PaginationUtils;
import com.server.server.utilities.FilterUtils;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import com.server.server.constants.BookingRules;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.exceptions.AccessDeniedException;

import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.models.agency.Agency;
import com.server.server.repositories.tourmanagement.TicketRepository;
import com.server.server.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourService extends GenericFilterService<Tour> {
    private final TourRepository tourRepository;
    private final TransportationService transportationService;
    private final AccessPolicyService accessPolicy;
    private final BookingLifecycleService bookingLifecycleService;
    private final GenericTrackingService trackingService;
    private final SystemSchedulingService schedulingService;
    private final NotificationService notificationService;
    private final TourReservationRepository reservationRepository;
    private final CountryRepository countryRepository;
    private final CityRepository cityRepository;
    private final MealPlanRepository mealPlanRepository;
    private final TicketRepository ticketRepository;

    @PostConstruct
    public void init() {
        schedulingService.registerNightlyTask(this::autoProcessScheduledTours);
    }

    // 3. Add the processing logic
    @Transactional
    public void autoProcessScheduledTours() {
        LocalDate today = LocalDate.now();

        // Task A: Auto-Cancel tours that passed their start date but are still PENDING
        List<Tour> pendingExpiredTours = tourRepository.findByStartDateBeforeAndStatus(today, GenericStatus.PENDING);

        for (Tour tour : pendingExpiredTours) {
            try {
                tour.setStatus(GenericStatus.CANCELLED);
                tour.setAvailableSlots(0); // Wipe inventory
                Tour savedTour = tourRepository.save(tour);

                // Log the system event (passing null for actor implies the System did it)
                trackingService.logEvent(
                        savedTour.getId(),
                        ReferenceType.TOUR,
                        "SYSTEM_AUTO_CANCEL",
                        "System automatically cancelled the tour because it passed the start date while still PENDING.",
                        null);

                log.info("System auto-cancelled expired pending tour: {}", tour.getTourNumber());
            } catch (Exception e) {
                log.error("Failed to auto-cancel tour {}: {}", tour.getTourNumber(), e.getMessage());
            }
        }

        // Task B: Auto-Complete tours that passed their end date and are
        // ACTIVE/APPROVED
        List<Tour> finishedTours = tourRepository.findByEndDateBeforeAndStatusIn(
                today,
                List.of(GenericStatus.ACTIVE, GenericStatus.APPROVED));

        for (Tour tour : finishedTours) {
            try {
                tour.setStatus(GenericStatus.COMPLETED);
                Tour savedTour = tourRepository.save(tour);

                // Log the system event
                trackingService.logEvent(
                        savedTour.getId(),
                        ReferenceType.TOUR,
                        "SYSTEM_AUTO_COMPLETE",
                        "System automatically marked tour as COMPLETED because the end date passed.",
                        null);

                // --- OPTIMIZED GREETING LOGIC ---
                if (savedTour.getTickets() != null) {
                    // Use a Set to prevent spamming a user who bought multiple tickets
                    java.util.Set<Integer> notifiedUsers = new java.util.HashSet<>();

                    for (Ticket ticket : savedTour.getTickets()) {
                        boolean eligibleForGreeting = ticket.getApprovalStatus() == GenericStatus.APPROVED
                                && ticket.getCustomer() != null;
                        if (eligibleForGreeting) {
                            Integer customerId = ticket.getCustomer().getId();

                            // Only send if we haven't already notified this specific user for this tour
                            if (!notifiedUsers.contains(customerId)) {
                                notificationService.sendTourCompletionGreeting(ticket);
                                notifiedUsers.add(customerId);
                            }
                        }
                        if (ticket.getTicketStatus() != TicketStatus.CANCELLED) {
                            ticket.setTicketStatus(TicketStatus.COMPLETED);
                            ticket.setApprovalStatus(GenericStatus.COMPLETED);
                        }
                    }
                }

                log.info("System auto-completed tour: {}", tour.getTourNumber());
            } catch (Exception e) {
                log.error("Failed to auto-complete tour {}: {}", tour.getTourNumber(), e.getMessage());
            }
        }
    }

    /** Admins and agency staff list every tour (staff within their agency); everyone else only bookable ones. */
    @Transactional(readOnly = true)
    public PageResponse<Tour> getAll(Integer page, Integer size, String sortBy, String sortDir) {
        return PageResponse.from(tourRepository.findAll(visibleTours(accessPolicy.currentUserOrNull()),
                PaginationUtils.pageable(page, size, sortBy, sortDir, "startDate",
                        Set.of("id", "tourNumber", "title", "startDate", "endDate", "totalPrice"))));
    }

    /**
     * One tour by id for the public endpoint. Bookable (ACTIVE) tours are public; others are shown to
     * administrators, the owning agency's staff, and customers holding a ticket for it. Anyone else gets 404,
     * so pending or cancelled tours do not leak.
     */
    @Transactional(readOnly = true)
    public Tour getVisible(@NonNull Integer id) {
        Tour tour = getById(id);
        if (tour.getStatus() == GenericStatus.ACTIVE) {
            return tour;
        }
        User current = accessPolicy.currentUserOrNull();
        boolean allowed = current != null && (AccessPolicyService.isAdmin(current)
                || (AccessPolicyService.isAgencyStaff(current) && current.getAgency() != null && tour.getAgency() != null
                        && Objects.equals(current.getAgency().getId(), tour.getAgency().getId()))
                || ticketRepository.existsByTour_IdAndCustomer_Id(id, current.getId()));
        if (!allowed) {
            throw new ResourceNotFoundException("Tour", id);
        }
        return tour;
    }

    /** Admins see all tours, agency staff their agency's, everyone else only ACTIVE (bookable) tours. */
    private Specification<Tour> visibleTours(User current) {
        if (current != null && AccessPolicyService.isAdmin(current)) {
            return (r, q, cb) -> cb.conjunction();
        }
        Integer scope = current == null ? null : AccessPolicyService.agencyScope(current);
        if (scope != null) {
            return (r, q, cb) -> cb.equal(r.get("agency").get("id"), scope);
        }
        return (r, q, cb) -> cb.equal(r.get("status"), GenericStatus.ACTIVE);
    }

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

        // Multi-Tenancy
        if (currentUser.getUserType() == UserTypeEnum.EMPLOYEE ||
                currentUser.getUserType() == UserTypeEnum.MANAGER ||
                currentUser.getUserType() == UserTypeEnum.OWNER) {

            tour.setAgency(currentUser.getAgency());

            if (currentUser.getUserType() != UserTypeEnum.OWNER || currentUser.getAgencyBranch() != null) {
                tour.setAgencyBranch(currentUser.getAgencyBranch());
            }
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

    @Transactional(readOnly = true)
    public PageResponse<Tour> getCatalogTours(Integer startCountryId, Integer endCountryId, LocalDate start,
            LocalDate end, Integer page, Integer size) {
        return PageResponse.from(tourRepository.findToursForCatalog(startCountryId, endCountryId, start, end,
                PaginationUtils.pageable(page, size, "startDate", "asc", "startDate", Set.of("startDate"))));
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

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public Tour updateStatus(@NonNull Integer id, GenericStatus newStatus) {
        Objects.requireNonNull(id, "id must not be null");
        Tour tour = getById(id);
        requireCanManage(accessPolicy.requireCurrentUser(), tour);
        GenericStatus currentStatus = tour.getStatus();

        if (currentStatus == newStatus)
            return tour;
        DomainWorkflowValidator.validateTour(currentStatus, newStatus);

        // 1. Terminal State Protection
        if (currentStatus == GenericStatus.COMPLETED) {
            throw new WorkflowException("Cannot change the status of a completed tour.");
        }

        // 2. Cancellation Recovery
        if (currentStatus == GenericStatus.CANCELLED) {
            if (newStatus != GenericStatus.PENDING) {
                throw new WorkflowException("A cancelled tour can only be restored to PENDING status.");
            }
        }

        // --- NEW: CANCELLATION BUSINESS RULES ---
        if (newStatus == GenericStatus.CANCELLED) {
            // Rule A: Must be at least 14 days away
            if (tour.getStartDate() != null && tour.getStartDate().isBefore(LocalDate.now().plusDays(BookingRules.TOUR_CANCELLATION_MIN_DAYS))) {
                throw new WorkflowException("Cancellation denied: Tour is less than 2 weeks away.");
            }

            // Rule B: Booked capacity must be <= 1/3 of max capacity
            int maxCap = tour.getMaxCapacity() != null ? tour.getMaxCapacity() : 0;
            // If availableSlots is null (e.g., pending tour), treat as 0 bookings (maxCap)
            int avail = tour.getAvailableSlots() != null ? tour.getAvailableSlots() : maxCap;
            int bookedSeats = maxCap - avail;

            if (bookedSeats > maxCap * BookingRules.TOUR_CANCELLATION_MAX_BOOKED_RATIO) {
                throw new WorkflowException("Cancellation denied: Booked seats exceed 1/3 of total capacity.");
            }

            cancelAffectedReservations(tour);
            tour.setAvailableSlots(0);
        }

        // 3. Forward Progression Rules
        if (currentStatus == GenericStatus.PENDING) {
            if (newStatus != GenericStatus.APPROVED && newStatus != GenericStatus.CANCELLED) {
                throw new WorkflowException("A pending tour must be APPROVED or CANCELLED.");
            }
        } else if (currentStatus == GenericStatus.APPROVED) {
            if (newStatus != GenericStatus.ACTIVE && newStatus != GenericStatus.CANCELLED) {
                throw new WorkflowException("An approved tour must be made ACTIVE or CANCELLED.");
            }
        } else if (currentStatus == GenericStatus.ACTIVE) {
            if (newStatus != GenericStatus.COMPLETED && newStatus != GenericStatus.CANCELLED) {
                throw new WorkflowException("An active tour must be marked COMPLETED or CANCELLED.");
            }
        }

        // Initialize inventory when going ACTIVE
        if (newStatus == GenericStatus.ACTIVE && currentStatus == GenericStatus.APPROVED) {
            tour.setAvailableSlots(tour.getMaxCapacity());
        }

        tour.setStatus(newStatus);
        return tourRepository.save(tour);
    }

    /** Agency cancellation: every active booking is cancelled and paid tickets are fully refunded. */
    private void cancelAffectedReservations(Tour tour) {
        List<TourReservation> reservations = reservationRepository.findByTour_IdAndStatusIn(
                tour.getId(), List.of(GenericStatus.PENDING, GenericStatus.APPROVED, GenericStatus.CONFIRMED));
        for (TourReservation reservation : reservations) {
            if (reservation.getStatus() == GenericStatus.CONFIRMED) {
                bookingLifecycleService.refundToWallet(reservation, BookingLifecycleService.activeTotal(reservation),
                        TransactionType.REFUND, "Full refund because the tour was cancelled by the agency");
            }
            bookingLifecycleService.releaseAndCancelTickets(reservation, GenericStatus.CANCELLED);
            reservation.setStatus(GenericStatus.CANCELLED);
            reservation.setHoldExpiresAt(null);
            reservationRepository.save(reservation);
            notificationService.sendNotification(reservation.getUser(), "Tour Cancelled",
                    "The tour '" + tour.getTitle() + "' was cancelled. Any paid amount was returned to your wallet.",
                    NotificationType.CANCELLATION_ALERT, reservation.getId(), ReferenceType.RESERVATION);
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

    private void requireCanManage(User user, Tour tour) {
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

    @Transactional(readOnly = true)
    public PageResponse<Tour> filter(TourFilterRequest filter) {

        // Use the centralized method
        User currentUser = accessPolicy.currentUserOrNull();

        // 3. Dynamic Filter Override
        if (currentUser != null) {
            if (currentUser.getUserType() == UserTypeEnum.EMPLOYEE
                    || currentUser.getUserType() == UserTypeEnum.MANAGER) {
                filter.setAgencyId(currentUser.getAgency() != null ? currentUser.getAgency().getId().longValue() : null);
                filter.setBranchId(currentUser.getAgencyBranch() != null ? currentUser.getAgencyBranch().getId().longValue() : null);
            } else if (currentUser.getUserType() == UserTypeEnum.OWNER) {
                filter.setAgencyId(currentUser.getAgency() != null ? currentUser.getAgency().getId().longValue() : null);
            }
        }

        Specification<Tour> spec = visibleTours(currentUser).and(hasStatus(filter.getStatus()))
                .and(hasMinSlots(filter.getMinSlots()))
                .and(FilterUtils.localDateRange("startDate", filter.getStartDate(), filter.getEndDate()))
                .and(hasAgency(filter.getAgencyId()))
                .and(hasBranch(filter.getBranchId()))
                .and(hasPriceBetween(filter.getMinPrice(), filter.getMaxPrice()))
                .and(hasCity(filter.getCityId()))
                .and(hasCountry(filter.getCountryId()))
                .and(hasCreatedBy(filter.getCreatedById()))
                .and(matchesSearch(filter.getSearch()));

        return executeFilter(tourRepository, spec, filter, "startDate",
                Set.of("id", "tourNumber", "title", "startDate", "endDate", "totalPrice", "availableSlots", "agency.name", "agencyBranch.name"),
                Map.of("agency", "agency.name", "branch", "agencyBranch.name"));
    }

    private Specification<Tour> hasBranch(Long b) {
        return (r, q, cb) -> b == null ? cb.conjunction() : cb.equal(r.get("agencyBranch").get("id"), b);
    }

    private Specification<Tour> hasStatus(GenericStatus s) {
        return (r, q, cb) -> s == null ? cb.conjunction() : cb.equal(r.get("status"), s);
    }

    private Specification<Tour> hasMinSlots(Integer m) {
        return (r, q, cb) -> m == null ? cb.conjunction() : cb.greaterThanOrEqualTo(r.get("availableSlots"), m);
    }

    private Specification<Tour> hasAgency(Long a) {
        return (r, q, cb) -> a == null ? cb.conjunction() : cb.equal(r.get("agency").get("id"), a);
    }

    private Specification<Tour> hasPriceBetween(Double min, Double max) {
        return (r, q, cb) -> {
            if (min != null && max != null)
                return cb.between(r.get("totalPrice"), min, max);
            if (min != null)
                return cb.greaterThanOrEqualTo(r.get("totalPrice"), min);
            if (max != null)
                return cb.lessThanOrEqualTo(r.get("totalPrice"), max);
            return cb.conjunction();
        };
    }

    private Specification<Tour> hasCity(Integer cityId) {
        return (r, q, cb) -> {
            if (cityId == null)
                return cb.conjunction();
            return cb.or(cb.equal(r.get("startCity").get("id"), cityId), cb.equal(r.get("endCity").get("id"), cityId));
        };
    }

    private Specification<Tour> hasCountry(Integer countryId) {
        return (r, q, cb) -> {
            if (countryId == null)
                return cb.conjunction();
            Join<Object, Object> destinations = r.join("destinationCountries", JoinType.LEFT);
            return cb.or(
                    cb.equal(r.get("startCountry").get("id"), countryId),
                    cb.equal(r.get("endCountry").get("id"), countryId),
                    cb.equal(destinations.get("id"), countryId));
        };
    }

    private Specification<Tour> hasCreatedBy(Integer createdById) {
        return (r, q, cb) -> createdById == null ? cb.conjunction()
                : cb.equal(r.get("createdBy").get("id"), createdById);
    }

    private Specification<Tour> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String term = normalizeSearch(search);
            return cb.or(
                    cb.like(cb.lower(root.get("tourNumber")), term),
                    cb.like(cb.lower(root.get("title")), term),
                    cb.like(cb.lower(root.get("description")), term));
        };
    }

}
