package com.server.server.services.tourmanagement;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import com.server.server.constants.errors.WorkflowErrorCodes;
import com.server.server.dto.tourmanagement.RefundQuoteResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.dto.common.PageResponse;
import com.server.server.dto.tourmanagement.TicketCreateRequest;
import com.server.server.dto.tourmanagement.TicketFilterRequest;
import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.notification.NotificationType;
import com.server.server.enums.notification.ReferenceType;
import com.server.server.enums.payment.TransactionType;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.exceptions.common.ResourceNotFoundException;
import com.server.server.exceptions.common.WorkflowException;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.models.user.User;
import com.server.server.repositories.tourmanagement.TicketRepository;
import com.server.server.repositories.user.UserRepository;
import com.server.server.services.auth.AccessPolicyService;
import com.server.server.services.common.GenericFilterService;
import com.server.server.services.common.SystemSchedulingService;
import com.server.server.services.notification.NotificationService;
import com.server.server.utilities.filters.FilterUtils;
import com.server.server.utilities.pagination.PaginationUtils;
import com.server.server.utilities.pricing.PricingCalculator;
import com.server.server.utilities.pricing.PricingPolicy;
import com.server.server.utilities.pricing.PricingCalculator.RefundQuote;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService extends GenericFilterService<Ticket> {

    private static final Set<String> SORTABLE = Set.of(
            "id", "ticketNumber", "bookingDate", "totalPrice", "tour.startDate", "tour.endDate");
    private static final Map<String, String> SORT_ALIASES = Map.of(
            "startDate", "tour.startDate", "endDate", "tour.endDate");

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final TourService tourService;
    private final TourReservationService reservationService;
    private final NotificationService notificationService;
    private final SystemSchedulingService schedulingService;
    private final InventoryService inventoryService;
    private final TicketIssuingService ticketIssuingService;
    private final BookingLifecycleService bookingLifecycleService;
    private final AccessPolicyService accessPolicy;

    @PostConstruct
    public void init() {
        schedulingService.registerNightlyTask(this::autoCancelExpiredTickets);
    }

    @Transactional(readOnly = true)
    public PageResponse<Ticket> getAll(Integer page, Integer size, String sortDir) {
        return PageResponse.from(ticketRepository.findAll(visibleTo(accessPolicy.requireCurrentUser()),
                PaginationUtils.pageable(page, size, "bookingDate", sortDir, "bookingDate", Set.of("bookingDate"))));
    }

    @Transactional(readOnly = true)
    public Ticket getById(@NonNull Integer id) {
        Ticket ticket = ticketRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
        accessPolicy.requireRecordAccess(accessPolicy.requireCurrentUser(), ticket.getCustomer().getId(),
                ticket.getTour().getAgency());
        return ticket;
    }

    /** What cancelling this ticket now would refund; nothing for an unpaid or cancelled ticket. */
    @Transactional(readOnly = true)
    public RefundQuoteResponse refundQuote(@NonNull Integer id) {
        Ticket ticket = getById(id);
        Tour tour = ticket.getTour();
        long days = tour.getStartDate() == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), tour.getStartDate());
        if (!ticket.isPaid() || ticket.getTicketStatus() == TicketStatus.CANCELLED) {
            return RefundQuoteResponse.of(BigDecimal.ZERO, new RefundQuote(PricingCalculator.money(BigDecimal.ZERO),
                    "Nothing was paid for this ticket, so nothing is refunded."), days);
        }
        return RefundQuoteResponse.of(ticket.getTotalPrice(), PricingCalculator.refund(ticket.getTotalPrice(),
                tour.getStartDate(), LocalDate.now(), PricingPolicy.of(tour.getAgency())), days);
    }

    /** Staff progress a paid ticket: APPROVED, CONFIRMED, or COMPLETED. Cancellation has its own rules. */
    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'OWNER')")
    public Ticket updateStatus(@NonNull Integer id, GenericStatus newStatus) {
        if (newStatus == GenericStatus.CANCELLED) {
            return cancelTicket(id);
        }
        Ticket ticket = getById(id);
        if (ticket.getTicketStatus() == TicketStatus.CANCELLED || ticket.getTicketStatus() == TicketStatus.COMPLETED) {
            throw new WorkflowException("A cancelled or completed ticket cannot change status.");
        }
        if (newStatus != GenericStatus.APPROVED && newStatus != GenericStatus.CONFIRMED
                && newStatus != GenericStatus.COMPLETED) {
            throw new WorkflowException("Ticket status can only progress to APPROVED, CONFIRMED, or COMPLETED.");
        }
        if (!ticket.isPaid()) {
            throw new WorkflowException(WorkflowErrorCodes.TICKET_NOT_PAID, "Cannot approve this ticket before it is paid.");
        }
        if (newStatus == GenericStatus.COMPLETED && ticket.getTour().getStatus() != GenericStatus.COMPLETED) {
            throw new WorkflowException("A ticket can only be completed after the tour is completed.");
        }
        ticket.setApprovalStatus(newStatus);
        if (newStatus == GenericStatus.CONFIRMED) {
            ticket.setTicketStatus(TicketStatus.CONFIRMED);
        } else if (newStatus == GenericStatus.COMPLETED) {
            ticket.setTicketStatus(TicketStatus.COMPLETED);
        }
        return ticketRepository.save(ticket);
    }

    /**
     * Cancels one ticket. A paid ticket is refunded by the notice-period policy; an unpaid one
     * lowers the pending reservation total. The reservation is cancelled with its last ticket.
     */
    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER', 'CUSTOMER')")
    public Ticket cancelTicket(@NonNull Integer ticketId) {
        Ticket ticket = getById(ticketId);
        if (ticket.getTicketStatus() == TicketStatus.CANCELLED) {
            throw new WorkflowException("Ticket is already cancelled.");
        }
        if (ticket.getTicketStatus() == TicketStatus.COMPLETED) {
            throw new WorkflowException("A completed ticket cannot be cancelled.");
        }
        Tour tour = ticket.getTour();
        if (tour.getStatus() == GenericStatus.COMPLETED) {
            throw new WorkflowException("Cannot cancel a ticket for a completed tour.");
        }
        TourReservation reservation = ticket.getReservation();
        if (ticket.isPaid() && reservation != null) {
            RefundQuote quote = PricingCalculator.refund(ticket.getTotalPrice(), tour.getStartDate(), LocalDate.now(),
                    PricingPolicy.of(tour.getAgency()));
            bookingLifecycleService.refundToWallet(reservation, quote.amount(), TransactionType.PARTIAL_REFUND,
                    quote.reason() + " for ticket " + ticket.getTicketNumber());
        }
        inventoryService.release(tour.getId(), 1, List.of(ticket));
        ticket.setTicketStatus(TicketStatus.CANCELLED);
        ticket.setApprovalStatus(GenericStatus.CANCELLED);
        Ticket saved = ticketRepository.save(ticket);
        if (reservation != null) {
            reservationService.syncAfterTicketCancellation(reservation);
        }
        notificationService.sendNotification(ticket.getCustomer(), "Ticket Cancelled",
                "Your ticket " + ticket.getTicketNumber() + " for tour '" + tour.getTitle() + "' has been cancelled.",
                NotificationType.CANCELLATION_ALERT, ticket.getId(), ReferenceType.TICKET);
        return saved;
    }

    /** Staff-issued walk-in ticket for an existing customer. It stays pending until paid. */
    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public Ticket create(TicketCreateRequest request) {
        Tour tour = tourService.getById(request.tour().id());
        accessPolicy.requireAgencyAccess(accessPolicy.requireCurrentUser(), tour.getAgency());
        User customer = userRepository.findById(request.customer().id())
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User", request.customer().id()));
        Ticket ticket = ticketIssuingService.issue(tour, customer, null, List.of(request.selection())).get(0);
        inventoryService.reserve(tour.getId(), 1, List.of(ticket));
        ticketIssuingService.applyPricing(ticket, tour);
        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public PageResponse<Ticket> filter(TicketFilterRequest filter) {
        Specification<Ticket> spec = visibleTo(accessPolicy.requireCurrentUser())
                .and(hasStatus(filter.getStatus()))
                .and(hasCustomer(filter.getCustomerId()))
                .and(hasTour(filter.getTourId()))
                .and(hasReservation(filter.getReservationId()))
                .and(FilterUtils.localDateTimeRange("bookingDate", filter.getStartDate(), filter.getEndDate()))
                .and(matchesSearch(filter.getSearch()));
        return executeFilter(ticketRepository, spec, filter, "bookingDate", SORTABLE, SORT_ALIASES);
    }

    /** Nightly: unpaid tickets whose tour has started are cancelled and their places released. */
    @Transactional
    public void autoCancelExpiredTickets() {
        for (Ticket ticket : ticketRepository.findAllPendingByDateBefore(LocalDate.now())) {
            try {
                ticket.setTicketStatus(TicketStatus.CANCELLED);
                ticket.setApprovalStatus(GenericStatus.CANCELLED);
                inventoryService.release(ticket.getTour().getId(), 1, List.of(ticket));
                ticketRepository.save(ticket);
                notificationService.sendTicketAutoCancellationNotification(ticket);
                log.info("System auto-cancelled expired ticket: {}", ticket.getTicketNumber());
            } catch (RuntimeException e) {
                log.error("Failed to auto-cancel ticket {}: {}", ticket.getTicketNumber(), e.getMessage());
            }
        }
    }

    private static Specification<Ticket> visibleTo(User user) {
        if (AccessPolicyService.isCustomer(user)) {
            return (r, q, cb) -> cb.equal(r.get("customer").get("id"), user.getId());
        }
        Integer agencyId = AccessPolicyService.agencyScope(user);
        return (r, q, cb) -> agencyId == null ? cb.conjunction()
                : cb.equal(r.get("tour").get("agency").get("id"), agencyId);
    }

    private Specification<Ticket> hasStatus(TicketStatus s) {
        return (r, q, cb) -> s == null ? cb.conjunction() : cb.equal(r.get("ticketStatus"), s);
    }

    private Specification<Ticket> hasCustomer(Long c) {
        return (r, q, cb) -> c == null ? cb.conjunction() : cb.equal(r.get("customer").get("id"), c);
    }

    private Specification<Ticket> hasReservation(Integer reservationId) {
        return (r, q, cb) -> reservationId == null ? cb.conjunction()
                : cb.equal(r.get("reservation").get("id"), reservationId);
    }

    private Specification<Ticket> hasTour(Integer t) {
        return (r, q, cb) -> t == null ? cb.conjunction() : cb.equal(r.get("tour").get("id"), t);
    }

    private Specification<Ticket> matchesSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return cb.conjunction();
            String term = normalizeSearch(search);
            return cb.or(
                    cb.like(cb.lower(root.get("ticketNumber")), term),
                    cb.like(cb.lower(root.get("customer").get("name")), term),
                    cb.like(cb.lower(root.get("tour").get("title")), term));
        };
    }
}
