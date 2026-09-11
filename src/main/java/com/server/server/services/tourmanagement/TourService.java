package com.server.server.services.tourmanagement;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.dto.filter.TourFilterRequest;
import com.server.server.services.filter.GenericFilterService;
import java.util.Set;
import java.util.Map;
import com.server.server.enums.Notification.ReferenceType;
import com.server.server.enums.UserTypeEnum;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.User;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.repositories.tourmanagement.TourRepository;
import com.server.server.services.GenericTrackingService;
import com.server.server.services.NotificationService;
import com.server.server.services.SystemSchedulingService;
import com.server.server.services.UserService;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourService extends GenericFilterService<Tour> {
    private final TourRepository tourRepository;
    private final TransportationService transportationService;
    private final UserService userService;
    private final GenericTrackingService trackingService;
    private final SystemSchedulingService schedulingService;
    private final NotificationService notificationService;

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
                        if (ticket.getApprovalStatus() == GenericStatus.APPROVED && ticket.getCustomer() != null) {
                            Integer customerId = ticket.getCustomer().getId();

                            // Only send if we haven't already notified this specific user for this tour
                            if (!notifiedUsers.contains(customerId)) {
                                notificationService.sendTourCompletionGreeting(ticket);
                                notifiedUsers.add(customerId);
                            }
                        }
                    }
                }

                log.info("System auto-completed tour: {}", tour.getTourNumber());
            } catch (Exception e) {
                log.error("Failed to auto-complete tour {}: {}", tour.getTourNumber(), e.getMessage());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<Tour> getAll() {
        return tourRepository.findAllWithDetails();
    }

    @Transactional(readOnly = true)
    public Tour getById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return tourRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found with id: " + id));
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public Tour create(Tour tour) {
        User currentUser = userService.getCurrentUser();

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

        validateTourDates(tour.getStartDate(), tour.getEndDate());
        double base = tour.getBasePrice() != null ? tour.getBasePrice() : 0.0;
        double discount = tour.getDiscountPrice() != null ? tour.getDiscountPrice() : 0.0;
        tour.setTotalPrice(base - discount);
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
    public Tour updateTour(@NonNull Integer id, Tour incomingData) {
        Objects.requireNonNull(id, "id must not be null");
        Tour existing = getById(id);
        User currentUser = userService.getCurrentUser();
        boolean canEdit = false;
        if (currentUser.getUserType() == UserTypeEnum.ADMIN) {
            canEdit = true;
        } else if (existing.getCreatedBy() != null && existing.getCreatedBy().getId().equals(currentUser.getId())) {
            canEdit = true;
        } else if (currentUser.getUserType() == UserTypeEnum.MANAGER &&
                existing.getAgencyBranch() != null &&
                existing.getAgencyBranch().getId().equals(currentUser.getAgencyBranch().getId())) {
            canEdit = true;
        } else if (currentUser.getUserType() == UserTypeEnum.OWNER &&
                existing.getAgency() != null &&
                existing.getAgency().getId().equals(currentUser.getAgency().getId())) {
            canEdit = true;
        }

        if (!canEdit) {
            throw new WorkflowException(
                    "Access Denied: You can only edit tours you created, or tours within your managed branch/agency.");
        }

        if (existing.getStatus() == GenericStatus.COMPLETED) {
            throw new WorkflowException("Cannot modify a completed tour.");
        }

        if (incomingData.getTitle() != null)
            existing.setTitle(incomingData.getTitle());
        if (incomingData.getTourNumber() != null)
            existing.setTourNumber(incomingData.getTourNumber());
        if (incomingData.getDescription() != null)
            existing.setDescription(incomingData.getDescription());
        if (incomingData.getStartDate() != null)
            existing.setStartDate(incomingData.getStartDate());
        if (incomingData.getEndDate() != null)
            existing.setEndDate(incomingData.getEndDate());
        if (incomingData.getNumberOfDays() != null)
            existing.setNumberOfDays(incomingData.getNumberOfDays());

        LocalDate finalStart = incomingData.getStartDate() != null ? incomingData.getStartDate()
                : existing.getStartDate();
        LocalDate finalEnd = incomingData.getEndDate() != null ? incomingData.getEndDate() : existing.getEndDate();
        validateTourDates(finalStart, finalEnd);

        if (incomingData.getMaxCapacity() != null) {
            int capacityDifference = incomingData.getMaxCapacity() - existing.getMaxCapacity();
            existing.setMaxCapacity(incomingData.getMaxCapacity());
            existing.setAvailableSlots(existing.getAvailableSlots() + capacityDifference);
        }

        if (incomingData.getBasePrice() != null) {
            existing.setBasePrice(incomingData.getBasePrice());
            existing.setPrice(incomingData.getBasePrice());
        }

        if (incomingData.getStartCountry() != null)
            existing.setStartCountry(incomingData.getStartCountry());
        if (incomingData.getEndCountry() != null)
            existing.setEndCountry(incomingData.getEndCountry());
        if (incomingData.getStartCity() != null)
            existing.setStartCity(incomingData.getStartCity());
        if (incomingData.getEndCity() != null)
            existing.setEndCity(incomingData.getEndCity());

        if (incomingData.getTransportation() != null && incomingData.getTransportation().getId() != null) {
            existing.setTransportation(transportationService.getById(incomingData.getTransportation().getId()));
            existing.setHasTransportation(true);
        }

        if (incomingData.getDestinationCountries() != null) {
            existing.getDestinationCountries().clear();
            existing.getDestinationCountries().addAll(incomingData.getDestinationCountries());
        }

        if (incomingData.getAvailableMeals() != null) {
            existing.getAvailableMeals().clear();
            existing.getAvailableMeals().addAll(incomingData.getAvailableMeals());
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
    public List<Tour> getCatalogTours(Integer startCountryId, Integer endCountryId, LocalDate start, LocalDate end) {
        return tourRepository.findToursForCatalog(startCountryId, endCountryId, start, end);
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
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public Tour updateStatus(@NonNull Integer id, GenericStatus newStatus) {
        Objects.requireNonNull(id, "id must not be null");
        Tour tour = getById(id);
        GenericStatus currentStatus = tour.getStatus();

        if (currentStatus == newStatus)
            return tour;

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
            if (tour.getStartDate() != null && tour.getStartDate().isBefore(LocalDate.now().plusDays(14))) {
                throw new WorkflowException("Cancellation denied: Tour is less than 2 weeks away.");
            }

            // Rule B: Booked capacity must be <= 1/3 of max capacity
            int maxCap = tour.getMaxCapacity() != null ? tour.getMaxCapacity() : 0;
            // If availableSlots is null (e.g., pending tour), treat as 0 bookings (maxCap)
            int avail = tour.getAvailableSlots() != null ? tour.getAvailableSlots() : maxCap;
            int bookedSeats = maxCap - avail;

            if (bookedSeats > (maxCap / 3.0)) {
                throw new WorkflowException("Cancellation denied: Booked seats exceed 1/3 of total capacity.");
            }

            // Wipe available slots to prevent further bookings
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

    @Transactional
    public void restoreInventory(@NonNull Integer tourId, int slotsToRestore) {
        Objects.requireNonNull(tourId, "tourId must not be null");
        Tour tour = tourRepository.findByIdWithLock(tourId)
                .orElseThrow(() -> new IllegalArgumentException("Tour not found"));

        if (tour.getStatus() == GenericStatus.COMPLETED) {
            throw new WorkflowException("Tour is completed. Inventory cannot be modified.");
        }

        tour.setAvailableSlots(tour.getAvailableSlots() + slotsToRestore);
        tourRepository.save(tour);
    }

    @Transactional(readOnly = true)
    public List<Tour> filter(TourFilterRequest filter) {

        // Use the centralized method
        User currentUser = userService.getCurrentUser();

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

        Specification<Tour> spec = Specification.where(hasStatus(filter.getStatus()))
                .and(hasMinSlots(filter.getMinSlots()))
                .and(isBetweenDates(filter.getStartDate(), filter.getEndDate()))
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

    private Specification<Tour> isBetweenDates(LocalDate s, LocalDate e) {
        return (r, q, cb) -> (s == null || e == null) ? cb.conjunction() : cb.between(r.get("startDate"), s, e);
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
