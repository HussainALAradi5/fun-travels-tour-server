package com.server.server.services.tourmanagement;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import com.server.server.dto.tourmanagement.RefundQuoteResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.constants.tourmanagement.BookingRules;
import com.server.server.constants.common.CodePrefixes;
import com.server.server.dto.common.PageResponse;
import com.server.server.dto.tourmanagement.ReservationCreateRequest;
import com.server.server.dto.tourmanagement.ReservationFilterRequest;
import com.server.server.dto.tourmanagement.TicketSelectionRequest;
import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.notification.NotificationType;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.enums.payment.PaymentMethod;
import com.server.server.enums.payment.PaymentStatus;
import com.server.server.enums.payment.TransactionType;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.exceptions.common.ResourceNotFoundException;
import com.server.server.exceptions.common.WorkflowException;
import com.server.server.models.payment.Payment;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.models.user.User;
import com.server.server.repositories.tourmanagement.TourReservationRepository;
import com.server.server.repositories.user.UserRepository;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.services.common.GenericFilterService;
import com.server.server.services.notification.NotificationService;
import com.server.server.services.payment.PaymentService;
import com.server.server.utilities.codes.CodeGenerator;
import com.server.server.validations.workflow.DomainWorkflowValidator;
import com.server.server.utilities.filters.FilterUtils;
import com.server.server.utilities.pagination.PaginationUtils;
import com.server.server.utilities.pricing.PricingCalculator;
import com.server.server.utilities.pricing.PricingPolicy;
import com.server.server.utilities.pricing.PricingCalculator.RefundQuote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TourReservationService extends GenericFilterService<TourReservation> {

    private static final Set<String> SORTABLE = Set.of(
            "id", "reservationNumber", "requestedSlots", "totalPrice", "status", "bookingDate");

    private final TourReservationRepository repository;
    private final UserRepository userRepository;
    private final TourService tourService;
    private final NotificationService notificationService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;
    private final TicketIssuingService ticketIssuingService;
    private final BookingLifecycleService bookingLifecycleService;
    private final AccessPolicyService accessPolicy;

    @Transactional(readOnly = true)
    public PageResponse<TourReservation> getAll(Integer page, Integer size, String sortDir) {
        Specification<TourReservation> scope = visibleTo(accessPolicy.requireCurrentUser());
        return PageResponse.from(repository.findAll(scope, PaginationUtils.pageable(page, size, "bookingDate", sortDir,
                "bookingDate", Set.of("bookingDate"))));
    }

    @Transactional(readOnly = true)
    public TourReservation getById(@NonNull Integer id) {
        TourReservation reservation = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
        accessPolicy.requireRecordAccess(accessPolicy.requireCurrentUser(), reservation.getUser().getId(),
                reservation.getTour().getAgency());
        return reservation;
    }

    /** What cancelling the whole booking now would refund; nothing for an unpaid or cancelled booking. */
    @Transactional(readOnly = true)
    public RefundQuoteResponse refundQuote(@NonNull Integer id) {
        TourReservation reservation = getById(id);
        Tour tour = reservation.getTour();
        long days = tour.getStartDate() == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), tour.getStartDate());
        if (reservation.getStatus() != GenericStatus.CONFIRMED) {
            return RefundQuoteResponse.of(BigDecimal.ZERO, new RefundQuote(PricingCalculator.money(BigDecimal.ZERO),
                    "Nothing was paid, so nothing is refunded."), days);
        }
        BigDecimal paid = BookingLifecycleService.activeTotal(reservation);
        return RefundQuoteResponse.of(paid, PricingCalculator.refund(paid, tour.getStartDate(), LocalDate.now(),
                PricingPolicy.of(tour.getAgency())), days);
    }

    /**
     * Holds places and seats for {@link BookingRules#HOLD_MINUTES} minutes while payment is pending.
     * Every traveler receives a ticket priced from stored records.
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'EMPLOYEE', 'ADMIN', 'MANAGER', 'OWNER')")
    public TourReservation create(ReservationCreateRequest request) {
        User current = accessPolicy.requireCurrentUser();
        User customer = resolveCustomer(current, request);
        Tour tour = tourService.getById(request.tour().id());
        accessPolicy.requireAgencyAccess(current, tour.getAgency());
        validateNoDateConflicts(customer.getId(), tour.getStartDate(), tour.getEndDate());

        List<TicketSelectionRequest> selections = travelerSelections(request);
        TourReservation reservation = new TourReservation();
        reservation.setReservationNumber(CodeGenerator.generate(CodePrefixes.RESERVATION));
        reservation.setTour(tour);
        reservation.setUser(customer);
        reservation.setRequestedSlots(selections.size());
        reservation.setStatus(GenericStatus.PENDING);
        reservation.setHoldExpiresAt(LocalDateTime.now().plusMinutes(BookingRules.HOLD_MINUTES));

        List<Ticket> tickets = ticketIssuingService.issue(tour, customer, reservation, selections);
        inventoryService.reserve(tour.getId(), tickets.size(), tickets);
        // Seats may have been assigned automatically, so price after inventory is held.
        tickets.forEach(ticket -> ticketIssuingService.applyPricing(ticket, tour));
        reservation.setTickets(new ArrayList<>(tickets));
        reservation.setTotalPrice(BookingLifecycleService.activeTotal(reservation));

        TourReservation saved = repository.save(reservation);
        notificationService.sendNotification(saved.getUser(), "Reservation Initiated",
                "Your reservation " + saved.getReservationNumber()
                        + " is pending. Please complete payment to secure your seats.",
                NotificationType.BOOKING_CONFIRMED, saved.getId(), ReferenceType.RESERVATION);
        return saved;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'OWNER')")
    public TourReservation finalizeReservationWithPayment(@NonNull Integer reservationId, PaymentMethod method) {
        TourReservation reservation = repository.findByIdWithLock(reservationId)
                .orElseThrow(() -> new WorkflowException("RESERVATION_NOT_FOUND", "Reservation was not found."));
        accessPolicy.requireRecordAccess(accessPolicy.requireCurrentUser(), reservation.getUser().getId(),
                reservation.getTour().getAgency());
        if (reservation.getStatus() != GenericStatus.PENDING) {
            throw new WorkflowException("RESERVATION_NOT_PAYABLE",
                    "This reservation can no longer be paid because it is "
                            + reservation.getStatus().name().toLowerCase() + ".");
        }
        if (reservation.getHoldExpiresAt() != null && !reservation.getHoldExpiresAt().isAfter(LocalDateTime.now())) {
            expireReservation(reservation);
            throw new WorkflowException("RESERVATION_HOLD_EXPIRED",
                    "Your reservation hold expired. Please select your seats again.");
        }
        Tour tour = reservation.getTour();
        if (tour.getStartDate().isBefore(LocalDate.now()) || tour.getStatus() != GenericStatus.ACTIVE) {
            throw new WorkflowException("TOUR_NOT_BOOKABLE", "This tour is no longer available for booking.");
        }
        Payment payment = paymentService.executeTransaction(reservation, method);
        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new WorkflowException("PAYMENT_FAILED",
                    "We could not complete the payment. Please try another payment method.");
        }
        DomainWorkflowValidator.validateReservation(reservation.getStatus(), GenericStatus.CONFIRMED);
        List<Ticket> active = BookingLifecycleService.activeTickets(reservation);
        active.forEach(ticket -> {
            ticket.setApprovalStatus(GenericStatus.APPROVED);
            ticket.setTicketStatus(TicketStatus.CONFIRMED);
            ticket.setPaid(true);
        });
        reservation.setStatus(GenericStatus.CONFIRMED);
        reservation.setHoldExpiresAt(null);
        notificationService.sendNotification(reservation.getUser(), "Payment Successful",
                String.format("Payment of $%.2f for '%s' successful.", reservation.getTotalPrice(), tour.getTitle()),
                NotificationType.BOOKING_CONFIRMED, reservation.getId(), ReferenceType.RESERVATION);
        return repository.save(reservation);
    }

    @Scheduled(fixedDelayString = "${booking.hold-cleanup-ms:60000}")
    @Transactional
    public void expirePendingReservations() {
        repository.findByStatusAndHoldExpiresAtBefore(GenericStatus.PENDING, LocalDateTime.now())
                .forEach(this::expireReservation);
    }

    /** Customer or staff cancellation. Paid reservations are refunded by the notice-period policy. */
    @Transactional
    public TourReservation cancelReservation(@NonNull Integer reservationId) {
        TourReservation reservation = getById(reservationId);
        if (reservation.getStatus() == GenericStatus.CANCELLED) {
            throw new WorkflowException("ALREADY_CANCELLED", "Reservation is already cancelled.");
        }
        DomainWorkflowValidator.validateReservation(reservation.getStatus(), GenericStatus.CANCELLED);
        Tour tour = reservation.getTour();
        if (tour.getStatus() == GenericStatus.COMPLETED) {
            throw new WorkflowException("Cannot cancel a reservation for a tour that is already completed.");
        }
        if (reservation.getStatus() == GenericStatus.CONFIRMED) {
            RefundQuote quote = PricingCalculator.refund(BookingLifecycleService.activeTotal(reservation),
                    tour.getStartDate(), LocalDate.now(), PricingPolicy.of(tour.getAgency()));
            bookingLifecycleService.refundToWallet(reservation, quote.amount(), TransactionType.REFUND, quote.reason());
        }
        bookingLifecycleService.releaseAndCancelTickets(reservation, GenericStatus.CANCELLED);
        reservation.setStatus(GenericStatus.CANCELLED);
        reservation.setHoldExpiresAt(null);
        notificationService.sendNotification(reservation.getUser(), "Booking Cancelled",
                "Your reservation for '" + tour.getTitle() + "' has been successfully cancelled.",
                NotificationType.CANCELLATION_ALERT, reservation.getId(), ReferenceType.RESERVATION);
        return repository.save(reservation);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public TourReservation updateStatus(@NonNull Integer id, GenericStatus newStatus) {
        if (newStatus == GenericStatus.CANCELLED) {
            return cancelReservation(id);
        }
        TourReservation reservation = getById(id);
        DomainWorkflowValidator.validateReservationAdministrativeTransition(reservation.getStatus(), newStatus);
        if (newStatus == GenericStatus.REJECTED) {
            bookingLifecycleService.releaseAndCancelTickets(reservation, GenericStatus.REJECTED);
            reservation.setHoldExpiresAt(null);
        } else if (newStatus == GenericStatus.COMPLETED) {
            BookingLifecycleService.activeTickets(reservation).forEach(ticket -> {
                ticket.setTicketStatus(TicketStatus.COMPLETED);
                ticket.setApprovalStatus(GenericStatus.COMPLETED);
            });
        }
        reservation.setStatus(newStatus);
        return repository.save(reservation);
    }

    /** Recalculates slots and total after one ticket of a reservation is cancelled. */
    @Transactional
    public void syncAfterTicketCancellation(TourReservation reservation) {
        List<Ticket> active = BookingLifecycleService.activeTickets(reservation);
        if (active.isEmpty()) {
            reservation.setStatus(GenericStatus.CANCELLED);
            reservation.setHoldExpiresAt(null);
        } else if (reservation.getStatus() == GenericStatus.PENDING) {
            reservation.setRequestedSlots(active.size());
            reservation.setTotalPrice(BookingLifecycleService.activeTotal(reservation));
        }
        repository.save(reservation);
    }

    @Transactional(readOnly = true)
    public PageResponse<TourReservation> filter(ReservationFilterRequest filter) {
        User current = accessPolicy.requireCurrentUser();
        Specification<TourReservation> spec = visibleTo(current)
                .and(hasStatus(filter.getStatus()))
                .and(hasCustomer(filter.getCustomerId()))
                .and(hasAgency(filter.getAgencyId()))
                .and(FilterUtils.localDateTimeRange("bookingDate", filter.getStartDate(), filter.getEndDate()))
                .and(matchesSearch(filter.getSearch()));
        return executeFilter(repository, spec, filter, "bookingDate", SORTABLE);
    }

    private void expireReservation(TourReservation reservation) {
        if (reservation.getStatus() != GenericStatus.PENDING) {
            return;
        }
        bookingLifecycleService.releaseAndCancelTickets(reservation, GenericStatus.CANCELLED);
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

    private User resolveCustomer(User current, ReservationCreateRequest request) {
        if (AccessPolicyService.isCustomer(current)) {
            return current;
        }
        if (request.user() == null || request.user().id() == null) {
            throw new WorkflowException("CUSTOMER_REQUIRED", "A reservation customer is required.");
        }
        return userRepository.findById(request.user().id())
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User", request.user().id()));
    }

    private static List<TicketSelectionRequest> travelerSelections(ReservationCreateRequest request) {
        if (request.tickets() != null && !request.tickets().isEmpty()) {
            return request.tickets();
        }
        int slots = Objects.requireNonNullElse(request.requestedSlots(), 1);
        List<TicketSelectionRequest> selections = new ArrayList<>();
        for (int i = 0; i < slots; i++) {
            selections.add(TicketSelectionRequest.none());
        }
        return selections;
    }

    private void validateNoDateConflicts(@NonNull Integer userId, LocalDate startDate, LocalDate endDate) {
        LocalDate effectiveEndDate = endDate != null ? endDate : startDate;
        if (repository.hasOverlappingReservations(userId, startDate, effectiveEndDate)) {
            throw new WorkflowException("DATE_CONFLICT",
                    "You already have an active reservation that conflicts with these dates.");
        }
    }

    private static Specification<TourReservation> visibleTo(User user) {
        if (AccessPolicyService.isCustomer(user)) {
            return (r, q, cb) -> cb.equal(r.get("user").get("id"), user.getId());
        }
        Integer agencyId = AccessPolicyService.agencyScope(user);
        return (r, q, cb) -> agencyId == null ? cb.conjunction()
                : cb.equal(r.get("tour").get("agency").get("id"), agencyId);
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
