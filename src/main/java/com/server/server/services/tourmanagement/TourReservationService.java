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
import org.springframework.scheduling.annotation.Scheduled;

import com.server.server.enums.GenericStatus;
import com.server.server.dto.filter.ReservationFilterRequest;
import com.server.server.dto.PageResponse;
import com.server.server.services.filter.GenericFilterService;
import java.util.Set;
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
import com.server.server.services.UserService;
import com.server.server.enums.UserTypeEnum;
import com.server.server.utilities.DomainWorkflowValidator;
import com.server.server.utilities.PaginationUtils;
import com.server.server.utilities.FilterUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TourReservationService extends GenericFilterService<TourReservation> {
    private final TourReservationRepository repository;
    private final TourService tourService;
    private final SeatService seatService;
    private final MealPlanService mealPlanService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final CodeGenerationService codeGenerationService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final InventoryService inventoryService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public PageResponse<TourReservation> getAll(Integer page, Integer size, String sortDir) {
        var currentUser = userService.getCurrentUser();
        Specification<TourReservation> scope = currentUser.getUserType() == UserTypeEnum.CUSTOMER
                ? (root, query, cb) -> cb.equal(root.get("user").get("id"), currentUser.getId())
                : (root, query, cb) -> cb.conjunction();
        return PageResponse.from(repository.findAll(scope, PaginationUtils.pageable(page, size, "bookingDate", sortDir,
                "bookingDate", Set.of("bookingDate"))));
    }

    @Transactional(readOnly = true)
    public TourReservation getById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        TourReservation reservation = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Reservation not found"));
        var currentUser = userService.getCurrentUser();
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER
                && !Objects.equals(reservation.getUser().getId(), currentUser.getId())) {
            throw new WorkflowException("You are not authorized to access this reservation.");
        }
        return reservation;
    }

    @Transactional(readOnly = true)
    public void sendTravelReminders() {
        LocalDate reminderDate = LocalDate.now().plusWeeks(1);
        List<TourReservation> upcoming = repository.findAllByTourStartDateAndStatus(
                reminderDate, GenericStatus.CONFIRMED);

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
        var currentUser = userService.getCurrentUser();
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER) res.setUser(currentUser);
        if (res.getUser() == null || res.getUser().getId() == null) {
            throw new WorkflowException("A reservation customer is required.");
        }
        Tour tour = tourService.getById(res.getTour().getId());

        // --- FIXED: Enforce the Date Conflict Validation ---
        validateNoDateConflicts(res.getUser().getId(), tour.getStartDate(), tour.getEndDate());

        int requestedSlots = (res.getTickets() != null && !res.getTickets().isEmpty()) ? res.getTickets().size()
                : (res.getRequestedSlots() != null ? res.getRequestedSlots() : 1);

        if (requestedSlots <= 0) throw new WorkflowException("At least one ticket is required.");

        if (res.getReservationNumber() == null || res.getReservationNumber().isEmpty()) {
            res.setReservationNumber("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        res.setRequestedSlots(requestedSlots);
        res.setStatus(GenericStatus.PENDING);
        res.setHoldExpiresAt(LocalDateTime.now().plusMinutes(15));

        // Process tickets (This now correctly handles BigDecimal math)
        double reservationGrandTotal = processTicketsAndCalculateTotal(res, tour);

        // Capacity and selected seats are atomically held while payment is pending.
        inventoryService.reserve(tour.getId(), requestedSlots, res.getTickets());

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
        TourReservation res = repository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new WorkflowException("RESERVATION_NOT_FOUND", "Reservation was not found."));
        var currentUser = userService.getCurrentUser();
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER
                && !Objects.equals(res.getUser().getId(), currentUser.getId())) {
            throw new WorkflowException("You are not authorized to pay for this reservation.");
        }

        if (res.getStatus() != GenericStatus.PENDING) {
            throw new WorkflowException("RESERVATION_NOT_PAYABLE",
                    "This reservation can no longer be paid because it is " + res.getStatus().name().toLowerCase() + ".");
        }
        if (res.getHoldExpiresAt() != null && !res.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
            expireReservation(res);
            throw new WorkflowException("RESERVATION_HOLD_EXPIRED",
                    "Your reservation hold expired. Please select your seats again.");
        }
        if (res.getTour().getStartDate().isBefore(LocalDate.now())
                || res.getTour().getStatus() != GenericStatus.ACTIVE) {
            throw new WorkflowException("TOUR_NOT_BOOKABLE", "This tour is no longer available for booking.");
        }

        // Process the payment (This is the ONLY place this should be called)
        Payment paymentResult = paymentService.executeTransaction(res, method);

        if (paymentResult.getStatus() == PaymentStatus.COMPLETED) {
            DomainWorkflowValidator.validateReservation(res.getStatus(), GenericStatus.CONFIRMED);
            res.setStatus(GenericStatus.CONFIRMED);
            res.setHoldExpiresAt(null);
            inventoryService.confirmSeats(res.getTickets());

            // Confirm all tickets
            if (res.getTickets() != null) {
                res.getTickets().forEach(t -> {
                    t.setApprovalStatus(GenericStatus.APPROVED);
                    t.setTicketStatus(TicketStatus.CONFIRMED);
                    t.setPaid(true);
                });
            }

            notificationService.sendNotification(
                    res.getUser(),
                    "Payment Successful",
                    String.format("Payment of $%.2f for '%s' successful.", res.getTotalPrice(),
                            res.getTour().getTitle()),
                    NotificationType.BOOKING_CONFIRMED,
                    res.getId(),
                    ReferenceType.RESERVATION);
        } else if (paymentResult.getStatus() == PaymentStatus.PENDING) {
            log.info("Payment awaiting confirmation for reservation {}", res.getReservationNumber());
            return repository.save(res);
        } else {
            throw new WorkflowException("PAYMENT_FAILED",
                    "We could not complete the payment. Please try another payment method.");
        }

        return repository.save(res);
    }

    @Scheduled(fixedDelayString = "${booking.hold-cleanup-ms:60000}")
    @Transactional
    public void expirePendingReservations() {
        repository.findByStatusAndHoldExpiresAtBefore(GenericStatus.PENDING, LocalDateTime.now())
                .forEach(this::expireReservation);
    }

    private void expireReservation(TourReservation reservation) {
        if (reservation.getStatus() != GenericStatus.PENDING) return;
        inventoryService.release(reservation.getTour().getId(), reservation.getRequestedSlots(),
                reservation.getTickets());
        reservation.getTickets().forEach(ticket -> {
            ticket.setTicketStatus(TicketStatus.CANCELLED);
            ticket.setApprovalStatus(GenericStatus.CANCELLED);
        });
        reservation.getPayments().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .forEach(payment -> payment.setStatus(PaymentStatus.FAILED));
        reservation.setStatus(GenericStatus.CANCELLED);
        reservation.setHoldExpiresAt(null);
        repository.save(reservation);
        notificationService.sendNotification(reservation.getUser(), "Reservation Expired",
                "Your reservation hold for '" + reservation.getTour().getTitle()
                        + "' expired. No payment was taken.",
                NotificationType.CANCELLATION_ALERT, reservation.getId(), ReferenceType.RESERVATION);
    }

    @Transactional
    public TourReservation cancelReservation(@NonNull Integer reservationId) {
        Objects.requireNonNull(reservationId, "reservationId must not be null");
        TourReservation res = repository.findById(reservationId)
                .orElseThrow(() -> new WorkflowException("Reservation not found"));
        var currentUser = userService.getCurrentUser();
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER
                && !Objects.equals(res.getUser().getId(), currentUser.getId())) {
            throw new WorkflowException("You are not authorized to cancel this reservation.");
        }

        if (res.getStatus() == GenericStatus.CANCELLED) {
            throw new WorkflowException("Reservation is already cancelled.");
        }

        Tour tour = res.getTour();

        // --- NEW: Prevent cancelling a tour that has already happened ---
        if (tour.getStatus() == GenericStatus.COMPLETED) {
            throw new WorkflowException("Cannot cancel a reservation for a tour that is already completed.");
        }

        GenericStatus previousStatus = res.getStatus();
        // If they already paid, calculate the refund. Inventory was held at creation time.
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

        }

        if (previousStatus == GenericStatus.PENDING || previousStatus == GenericStatus.APPROVED
                || previousStatus == GenericStatus.CONFIRMED) {
            inventoryService.release(tour.getId(), res.getRequestedSlots(), res.getTickets());
        }
        if (res.getTickets() != null) res.getTickets().forEach(ticket -> {
            ticket.setTicketStatus(TicketStatus.CANCELLED);
            ticket.setApprovalStatus(GenericStatus.CANCELLED);
        });

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
        DomainWorkflowValidator.validateReservation(res.getStatus(), newStatus);
        res.setStatus(newStatus);
        return repository.save(res);
    }

    @Transactional(readOnly = true)
    public PageResponse<TourReservation> filter(ReservationFilterRequest filter) {
        var currentUser = userService.getCurrentUser();
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER) {
            filter.setCustomerId(currentUser.getId().longValue());
        }
        Specification<TourReservation> spec = Specification.where(hasStatus(filter.getStatus()))
                .and(hasCustomer(filter.getCustomerId()))
                .and(hasAgency(filter.getAgencyId()))
                .and(FilterUtils.localDateTimeRange("bookingDate", filter.getStartDate(), filter.getEndDate()))
                .and(matchesSearch(filter.getSearch()));
        return executeFilter(repository, spec, filter, "bookingDate",
                Set.of("id", "reservationNumber", "requestedSlots", "totalPrice", "status", "bookingDate"));
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

    private Specification<TourReservation> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String term = normalizeSearch(search);
            return cb.or(
                    cb.like(cb.lower(root.get("reservationNumber")), term),
                    cb.like(cb.lower(root.get("user").get("name")), term),
                    cb.like(cb.lower(root.get("tour").get("title")), term));
        };
    }
}
