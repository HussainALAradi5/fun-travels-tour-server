package com.server.server.services.tourmanagement;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.payment.TransactionType;
import com.server.server.enums.notification.NotificationType;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.repositories.tourmanagement.TourRepository;
import com.server.server.repositories.tourmanagement.TourReservationRepository;
import com.server.server.services.notification.NotificationService;
import com.server.server.utilities.DomainWorkflowValidator;
import com.server.server.constants.BookingRules;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.models.agency.Agency;
import lombok.RequiredArgsConstructor;

/** Tour status workflow (PENDING -> APPROVED -> ACTIVE -> COMPLETED, or CANCELLED) and its effects on bookings. */
@Service
@RequiredArgsConstructor
public class TourStatusService {

    private final TourService tourService;
    private final TourRepository tourRepository;
    private final AccessPolicyService accessPolicy;
    private final BookingLifecycleService bookingLifecycleService;
    private final NotificationService notificationService;
    private final TourReservationRepository reservationRepository;

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public Tour updateStatus(@NonNull Integer id, GenericStatus newStatus) {
        Objects.requireNonNull(id, "id must not be null");
        Tour tour = tourService.getById(id);
        tourService.requireCanManage(accessPolicy.requireCurrentUser(), tour);
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
}
