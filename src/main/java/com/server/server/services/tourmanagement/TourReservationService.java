package com.server.server.services.tourmanagement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.Notification.NotificationType;
import com.server.server.enums.Notification.ReferenceType;
import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.enums.Payment.PaymentStatus;
import com.server.server.enums.TransactionType;
import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.Account;
import com.server.server.models.Payment;
import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.repositories.tourmanagement.TourReservationRepository;
import com.server.server.services.Account.AccountService;
import com.server.server.services.CodeGenerationService;
import com.server.server.services.NotificationService;
import com.server.server.services.PaymentService;
import com.server.server.services.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TourReservationService {
    private final TourReservationRepository repository;
    private final TourService tourService;
    private final SeatService seatService;
    private final MealPlanService mealPlanService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final CodeGenerationService codeGenerationService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    @Transactional(readOnly = true)
    public List<TourReservation> getAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public TourReservation getById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
    }

    @Transactional(readOnly = true)
    public void sendTravelReminders() {
        LocalDate reminderDate = LocalDate.now().plusWeeks(1);
        List<TourReservation> upcoming = repository.findAllByTourStartDateAndStatus(
                reminderDate, GenericStatus.APPROVED);

        for (TourReservation res : upcoming) {
            String reminderMsg = String.format(
                    "Reminder: Your tour '%s' (Code: %s) starts in one week! Check your tickets for departure details.",
                    res.getTour().getTitle(),
                    res.getReservationNumber());

            notificationService.sendNotification(
                    res.getUser(),
                    "Travel Reminder",
                    reminderMsg,
                    NotificationType.ONE_WEEK_TRAVEL_REMINDER,
                    res.getId(),
                    ReferenceType.RESERVATION);
        }
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'EMPLOYEE', 'ADMIN', 'MANAGER')")
    public TourReservation create(TourReservation res) {
        Tour tour = tourService.getById(res.getTour().getId());

        // --- FIXED: Enforce the Date Conflict Validation ---
        validateNoDateConflicts(res.getUser().getId(), tour.getStartDate(), tour.getEndDate());

        int requestedSlots = (res.getTickets() != null && !res.getTickets().isEmpty()) ? res.getTickets().size()
                : (res.getRequestedSlots() != null ? res.getRequestedSlots() : 1);

        if (tour.getAvailableSlots() < requestedSlots) {
            throw new WorkflowException("Not enough available slots on this tour.");
        }

        if (res.getReservationNumber() == null || res.getReservationNumber().isEmpty()) {
            res.setReservationNumber("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        res.setRequestedSlots(requestedSlots);
        res.setStatus(GenericStatus.PENDING);

        // Process tickets (This now correctly handles BigDecimal math)
        double reservationGrandTotal = processTicketsAndCalculateTotal(res, tour);

        if (res.getTickets() != null && !res.getTickets().isEmpty()) {
            res.setTotalPrice(BigDecimal.valueOf(reservationGrandTotal));
        } else if (res.getTotalPrice() == null) {
            res.setTotalPrice(BigDecimal.ZERO);
        }

        TourReservation savedRes = repository.save(res);

        notificationService.sendNotification(
                savedRes.getUser(),
                "Reservation Initiated",
                "Your reservation " + savedRes.getReservationNumber()
                        + " is pending. Please complete payment to secure your seats.",
                NotificationType.BOOKING_CONFIRMED, savedRes.getId(), ReferenceType.RESERVATION);

        return savedRes;
    }

    private double processTicketsAndCalculateTotal(TourReservation res, Tour tour) {
        if (res.getTickets() == null || res.getTickets().isEmpty()) {
            return 0.0;
        }

        double reservationGrandTotal = 0.0;

        // Safely map tour base price to BigDecimal
        BigDecimal ticketBase = tour.getTotalPrice() != null
                ? new BigDecimal(tour.getTotalPrice().toString())
                : BigDecimal.ZERO;

        for (Ticket ticket : res.getTickets()) {
            // Relational Setup
            ticket.setTour(tour);
            ticket.setCustomer(res.getUser());
            ticket.setReservation(res);

            String uniqueTicketNumber = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            ticket.setTicketNumber(uniqueTicketNumber);
            ticket.setBookingDate(LocalDateTime.now());

            // --- GENERATE QR AND BARCODE ---
            try {
                String qrData = String.format("TICKET:%s|TOUR:%d|CUSTOMER:%d",
                        uniqueTicketNumber,
                        tour.getId(),
                        res.getUser().getId());

                ticket.setQrCode(codeGenerationService.generateQRCode(qrData, 250, 250));
                ticket.setBarcode(codeGenerationService.generateBarcode(uniqueTicketNumber, 300, 100));
            } catch (Exception e) {
                throw new WorkflowException("Failed to generate ticket codes for group booking: " + e.getMessage());
            }

            // Variables for Calculation
            BigDecimal ticketDiscount = BigDecimal.ZERO;
            BigDecimal seatModifier = BigDecimal.ZERO;
            BigDecimal mealsTotal = BigDecimal.ZERO;

            // Process Seat
            if (ticket.getAssignedSeat() != null && ticket.getAssignedSeat().getId() != null) {
                Seat seat = seatService.getById(ticket.getAssignedSeat().getId());
                ticket.setAssignedSeat(seat);

                if (seat.getSeatPriceModifier() != null) {
                    seatModifier = new BigDecimal(seat.getSeatPriceModifier().toString());
                }

                if (seat.getChairType() == ChairType.KIDS_CHAIR) {
                    ticketDiscount = ticketBase.multiply(new BigDecimal("0.50"));
                }
            }

            // Process Meals
            if (ticket.getSelectedMeals() != null && !ticket.getSelectedMeals().isEmpty()) {
                ticket.setHasMealPlan(true);
                for (MealPlan mealPayload : ticket.getSelectedMeals()) {
                    MealPlan dbMeal = mealPlanService.findById(mealPayload.getId());

                    boolean isAllowed = tour.getAvailableMeals().stream()
                            .anyMatch(m -> m.getId().equals(dbMeal.getId()));
                    if (!isAllowed) {
                        throw new WorkflowException("Meal '" + dbMeal.getMealName() + "' is not offered on this tour.");
                    }
                    if (dbMeal.getMealPrice() != null) {
                        mealsTotal = mealsTotal.add(new BigDecimal(dbMeal.getMealPrice().toString()));
                    }
                }
            } else {
                ticket.setHasMealPlan(false);
            }

            // --- FIXED: Set Pricing using BigDecimal ---
            ticket.setBasePrice(ticketBase);
            ticket.setDiscountPrice(ticketDiscount);
            ticket.setSeatPriceModifier(seatModifier);

            // Finalize Ticket Total: (Base - Discount) + SeatModifier + MealsTotal
            BigDecimal finalTicketPrice = ticketBase.subtract(ticketDiscount).add(seatModifier).add(mealsTotal);
            ticket.setTotalPrice(finalTicketPrice);

            // Accumulate grand total as a double for the main reservation scope
            reservationGrandTotal += finalTicketPrice.doubleValue();
        }

        return reservationGrandTotal;
    }

    private void validateNoDateConflicts(@NonNull Integer userId, LocalDate startDate, LocalDate endDate) {
        Objects.requireNonNull(userId, "userId must not be null");
        // Fallback to startDate if endDate is null (e.g., 1-day tours)
        LocalDate effectiveEndDate = endDate != null ? endDate : startDate;

        boolean hasConflict = repository.hasOverlappingReservations(userId, startDate, effectiveEndDate);
        if (hasConflict) {
            throw new WorkflowException("You already have an active reservation that conflicts with these dates.");
        }
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER')")
    public TourReservation finalizeReservationWithPayment(@NonNull Integer reservationId, PaymentMethod method) {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        TourReservation res = getById(reservationId);

        if (res.getStatus() == GenericStatus.APPROVED || res.getStatus() == GenericStatus.ACTIVE) {
            throw new WorkflowException("Reservation is already processed.");
        }

        // Process the payment (This is the ONLY place this should be called)
        Payment paymentResult = paymentService.executeTransaction(res, method);

        if (paymentResult.getStatus() == PaymentStatus.COMPLETED) {
            res.setStatus(GenericStatus.APPROVED);

            // Confirm all tickets
            if (res.getTickets() != null) {
                res.getTickets().forEach(t -> t.setApprovalStatus(GenericStatus.APPROVED));
            }

            notificationService.sendNotification(
                    res.getUser(),
                    "Payment Successful",
                    String.format("Payment of $%.2f for '%s' successful.", res.getTotalPrice(),
                            res.getTour().getTitle()),
                    NotificationType.BOOKING_CONFIRMED,
                    res.getId(),
                    ReferenceType.RESERVATION);
        } else {
            log.warn("Payment failed for Reservation: {}", res.getReservationNumber());
            throw new WorkflowException("Payment failed. Please try a different method.");
        }

        return repository.save(res);
    }

    @Transactional
    public TourReservation cancelReservation(@NonNull Integer reservationId) {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        TourReservation res = repository.findById(reservationId)
                .orElseThrow(() -> new WorkflowException("Reservation not found"));

        if (res.getStatus() == GenericStatus.CANCELLED) {
            throw new WorkflowException("Reservation is already cancelled.");
        }

        Tour tour = res.getTour();

        // --- NEW: Prevent cancelling a tour that has already happened ---
        if (tour.getStatus() == GenericStatus.COMPLETED) {
            throw new WorkflowException("Cannot cancel a reservation for a tour that is already completed.");
        }

        // If they already paid, we need to calculate refunds and restore inventory
        if (res.getStatus() == GenericStatus.CONFIRMED) {
            long daysUntilTour = ChronoUnit.DAYS.between(LocalDate.now(), tour.getStartDate());
            BigDecimal totalPaid = res.getTotalPrice();
            BigDecimal refundAmount = BigDecimal.ZERO;
            String refundReason = "";

            if (daysUntilTour > 14) {
                refundAmount = totalPaid; // Full Refund
                refundReason = "Full refund (> 2 weeks notice)";
            } else if (daysUntilTour >= 7) {
                refundAmount = totalPaid.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP); // 50% Refund
                refundReason = "50% Partial refund (1-2 weeks notice)";
            } else {
                refundAmount = BigDecimal.ZERO; // No Refund
                refundReason = "No refund (< 1 week notice). Cancellation fee applied.";
            }

            // Refund the money to their digital Wallet
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                Account userAccount = accountService.getAccountByUserId(res.getUser().getId());
                transactionService.creditAccount(userAccount, refundAmount, TransactionType.REFUND, refundReason, res);
            }

            // Restore tour inventory since the seats are freed up
            tourService.restoreInventory(tour.getId(), res.getRequestedSlots());

            // Free up the specific seats and cancel tickets
            if (res.getTickets() != null) {
                for (Ticket ticket : res.getTickets()) {
                    if (ticket.getAssignedSeat() != null) {
                        seatService.updateStatus(ticket.getAssignedSeat().getId(), SeatStatus.AVAILABLE);
                    }
                    ticket.setTicketStatus(TicketStatus.CANCELLED);
                    ticket.setApprovalStatus(GenericStatus.CANCELLED);
                }
            }
        }

        res.setStatus(GenericStatus.CANCELLED);

        // Send unified notification
        notificationService.sendNotification(
                res.getUser(), "Booking Cancelled",
                "Your reservation for '" + tour.getTitle() + "' has been successfully cancelled.",
                NotificationType.CANCELLATION_ALERT, res.getId(), ReferenceType.RESERVATION);

        return repository.save(res);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE')")
    public TourReservation updateStatus(@NonNull Integer id, GenericStatus newStatus) {
        Objects.requireNonNull(id, "id must not be null");
        if (newStatus == GenericStatus.CANCELLED) {
            return cancelReservation(id);
        }
        TourReservation res = getById(id);
        res.setStatus(newStatus);
        return repository.save(res);
    }

    @Transactional(readOnly = true)
    public List<TourReservation> filter(GenericStatus status, Long customerId, Long agencyId) {
        return repository
                .findAll(Specification.where(hasStatus(status)).and(hasCustomer(customerId)).and(hasAgency(agencyId)));
    }

    private Specification<TourReservation> hasStatus(GenericStatus s) {
        return (r, q, cb) -> s == null ? cb.conjunction() : cb.equal(r.get("status"), s);
    }

    private Specification<TourReservation> hasCustomer(Long c) {
        return (r, q, cb) -> c == null ? cb.conjunction() : cb.equal(r.get("user").get("id"), c);
    }

    private Specification<TourReservation> hasAgency(Long a) {
        return (r, q, cb) -> a == null ? cb.conjunction() : cb.equal(r.get("tour").get("agency").get("id"), a);
    }
}
