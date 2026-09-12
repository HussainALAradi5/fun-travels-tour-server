package com.server.server.services.tourmanagement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.Notification.NotificationType;
import com.server.server.enums.Notification.ReferenceType;
import com.server.server.enums.TransactionType;
import com.server.server.enums.tourmanagement.ChairType;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.dto.filter.TicketFilterRequest;
import com.server.server.dto.PageResponse;
import com.server.server.services.filter.GenericFilterService;
import java.util.Set;
import java.util.Map;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.Account;
import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.repositories.tourmanagement.TicketRepository;
import com.server.server.services.Account.AccountService;
import com.server.server.services.CodeGenerationService;
import com.server.server.services.NotificationService;
import com.server.server.services.SystemSchedulingService;
import com.server.server.services.TransactionService;
import com.server.server.services.UserService;
import com.server.server.enums.UserTypeEnum;
import com.server.server.utilities.PaginationUtils;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService extends GenericFilterService<Ticket> {
    private final TicketRepository ticketRepository;
    private final SeatService seatService;
    private final TourService tourService;
    private final NotificationService notificationService;
    private final MealPlanService mealPlanService;
    private final CodeGenerationService codeGenerationService;
    private final SystemSchedulingService schedulingService;
    private final AccountService accountService;
    private final TransactionService transactionService;
    private final InventoryService inventoryService;
    private final UserService userService;




    @Transactional(readOnly = true)
    public PageResponse<Ticket> getAll(Integer page, Integer size, String sortDir) {
        var currentUser = userService.getCurrentUser();
        Specification<Ticket> scope = currentUser.getUserType() == UserTypeEnum.CUSTOMER
                ? (root, query, cb) -> cb.equal(root.get("customer").get("id"), currentUser.getId())
                : (root, query, cb) -> cb.conjunction();
        return PageResponse.from(ticketRepository.findAll(scope, PaginationUtils.pageable(page, size, "bookingDate", sortDir,
                "bookingDate", Set.of("bookingDate"))));
    }

    @Transactional(readOnly = true)
    public Ticket getById(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        Ticket ticket = ticketRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
        var currentUser = userService.getCurrentUser();
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER
                && !Objects.equals(ticket.getCustomer().getId(), currentUser.getId())) {
            throw new WorkflowException("You are not authorized to access this ticket.");
        }
        return ticket;
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('CUSTOMER')")
    public Ticket update(@NonNull Integer id, Ticket incomingData) {
        Objects.requireNonNull(id, "id must not be null");
        Ticket existing = getById(id);

        // Allow updating the seat if the ticket isn't completed
        if (incomingData.getAssignedSeat() != null &&
                !incomingData.getAssignedSeat().getId().equals(existing.getAssignedSeat().getId())) {

            // Release old seat, lock new one
            seatService.updateStatus(existing.getAssignedSeat().getId(), SeatStatus.AVAILABLE);
            Seat newSeat = seatService.getById(incomingData.getAssignedSeat().getId());
            existing.setAssignedSeat(newSeat);
        }

        // Allow updating meals
        if (incomingData.getSelectedMeals() != null) {
            existing.setSelectedMeals(incomingData.getSelectedMeals());
        }

        // Recalculate everything based on the updated state
        calculateAndSetPricing(existing, existing.getTour());

        return ticketRepository.save(existing);
    }

@Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER')")
    public Ticket updateStatus(@NonNull Integer id, GenericStatus newStatus) {
        Objects.requireNonNull(id, "id must not be null");
        
        // 1. If the user or admin is cancelling, delegate to the cancellation engine
        if (newStatus == GenericStatus.CANCELLED) {
            return cancelTicket(id);
        }

        Ticket ticket = getById(id);

        // 2. BUSINESS RULE: "No Pay, No Approve"
        // If someone tries to approve or confirm the ticket, check if the reservation is paid
        if ((newStatus == GenericStatus.APPROVED || newStatus == GenericStatus.CONFIRMED)) {
            if (ticket.getReservation() != null && ticket.getReservation().getStatus() != GenericStatus.CONFIRMED) {
                throw new WorkflowException("Cannot approve this ticket. The associated reservation has not been paid yet.");
            }
        }

        ticket.setApprovalStatus(newStatus);
        
        // Keep TicketStatus in sync
        if (newStatus == GenericStatus.CONFIRMED) {
            ticket.setTicketStatus(TicketStatus.CONFIRMED);
        }

        return ticketRepository.save(ticket);
    }
@Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER')")
    public Ticket cancelTicket(@NonNull Integer ticketId) {
        Objects.requireNonNull(ticketId, "ticketId must not be null");
        Ticket ticket = getById(ticketId);
        var currentUser = userService.getCurrentUser();
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER
                && !Objects.equals(ticket.getCustomer().getId(), currentUser.getId())) {
            throw new WorkflowException("You are not authorized to cancel this ticket.");
        }

        if (ticket.getTicketStatus() == TicketStatus.CANCELLED) {
            throw new WorkflowException("Ticket is already cancelled.");
        }

        Tour tour = ticket.getTour();
        if (tour.getStatus() == GenericStatus.COMPLETED) {
            throw new WorkflowException("Cannot cancel a ticket for a completed tour.");
        }

        // SCENARIO A: The ticket was actually paid for (Reservation is CONFIRMED)
        if (ticket.getReservation() != null && ticket.getReservation().getStatus() == GenericStatus.CONFIRMED) {
            
            long daysUntilTour = ChronoUnit.DAYS.between(LocalDate.now(), tour.getStartDate());
            BigDecimal totalPaid = ticket.getTotalPrice() != null ? ticket.getTotalPrice() : BigDecimal.ZERO;
            BigDecimal refundAmount = BigDecimal.ZERO;
            String refundReason = "";

            // Calculate specific refund for this ONE ticket
            if (daysUntilTour > 14) {
                refundAmount = totalPaid;
                refundReason = "Full ticket refund (> 2 weeks notice)";
            } else if (daysUntilTour >= 7) {
                refundAmount = totalPaid.divide(BigDecimal.valueOf(2), RoundingMode.HALF_UP);
                refundReason = "50% Partial ticket refund (1-2 weeks notice)";
            } else {
                refundAmount = BigDecimal.ZERO;
                refundReason = "No refund for ticket (< 1 week notice).";
            }

            // Refund to Digital Wallet
            if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
                Account userAccount = accountService.getAccountByUserId(ticket.getCustomer().getId());
                transactionService.creditAccount(
                    userAccount, 
                    refundAmount, 
                    TransactionType.PARTIAL_REFUND, // Important: Using the PARTIAL enum
                    refundReason, 
                    ticket.getReservation()
                );
            }

        }

        // Both pending holds and sold tickets consumed one inventory unit.
        inventoryService.release(tour.getId(), 1, List.of(ticket));

        // Finalize the cancellation statuses
        ticket.setTicketStatus(TicketStatus.CANCELLED);
        ticket.setApprovalStatus(GenericStatus.CANCELLED);

        // Notify the customer
        notificationService.sendNotification(
            ticket.getCustomer(), 
            "Ticket Cancelled",
            "Your ticket " + ticket.getTicketNumber() + " for tour '" + tour.getTitle() + "' has been cancelled.",
            NotificationType.CANCELLATION_ALERT, 
            ticket.getId(), 
            ReferenceType.TICKET
        );

        return ticketRepository.save(ticket);
    }
    @Transactional
    public Ticket approveTicket(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return updateStatus(id, GenericStatus.APPROVED);
    }

    @Transactional
    public Ticket confirmTicket(@NonNull Integer id) {
        Objects.requireNonNull(id, "id must not be null");
        return updateStatus(id, GenericStatus.CONFIRMED);
    }

    @Transactional
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'OWNER')")
    public Ticket create(Ticket ticket) {
        // 1. Fetch required relations
        Tour tour = tourService.getById(ticket.getTour().getId());
        var currentUser = userService.getCurrentUser();
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER) ticket.setCustomer(currentUser);
        if (ticket.getCustomer() == null || ticket.getCustomer().getId() == null) {
            throw new WorkflowException("A ticket customer is required.");
        }

        // 2. Initialize Core Fields
        String uniqueTicketNumber = "TKT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ticket.setTour(tour);
        ticket.setTicketNumber(uniqueTicketNumber);
        ticket.setBookingDate(LocalDateTime.now());

        // 3. Generate QR and Barcode
        try {
            // QR Code can contain richer verification data
            String qrData = String.format("TICKET:%s|TOUR:%d|CUSTOMER:%d",
                    uniqueTicketNumber,
                    tour.getId(),
                    ticket.getCustomer().getId());

            ticket.setQrCode(codeGenerationService.generateQRCode(qrData, 250, 250));

            // Barcode usually just contains the ID/Number for scanners
            ticket.setBarcode(codeGenerationService.generateBarcode(uniqueTicketNumber, 300, 100));
        } catch (Exception e) {
            throw new WorkflowException("Failed to generate ticket codes: " + e.getMessage());
        }

        // 4. Apply business logic and pricing
        calculateAndSetPricing(ticket, tour);

        // 5. Inventory Management
        inventoryService.reserve(tour.getId(), 1, List.of(ticket));

        return ticketRepository.save(ticket);
    }

    @Transactional(readOnly = true)
    public PageResponse<Ticket> filter(TicketFilterRequest filter) {
        var currentUser = userService.getCurrentUser();
        if (currentUser.getUserType() == UserTypeEnum.CUSTOMER) {
            filter.setCustomerId(currentUser.getId().longValue());
        }
        Specification<Ticket> spec = Specification.where(hasStatus(filter.getStatus()))
                .and(hasCustomer(filter.getCustomerId()))
                .and(hasTour(filter.getTourId()))
                .and(matchesSearch(filter.getSearch()));

        return executeFilter(ticketRepository, spec, filter, "bookingDate",
                Set.of("id", "ticketNumber", "bookingDate", "totalPrice", "tour.startDate", "tour.endDate"),
                Map.of("startDate", "tour.startDate", "endDate", "tour.endDate"));
    }

    private Specification<Ticket> hasStatus(TicketStatus s) {
        return (r, q, cb) -> s == null ? cb.conjunction() : cb.equal(r.get("ticketStatus"), s);
    }

    private Specification<Ticket> hasCustomer(Long c) {
        return (r, q, cb) -> c == null ? cb.conjunction() : cb.equal(r.get("customer").get("id"), c);
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

private void calculateAndSetPricing(Ticket ticket, Tour tour) {
        // Safely convert Tour base price to BigDecimal
        BigDecimal base = tour.getTotalPrice() != null 
            ? new BigDecimal(tour.getTotalPrice().toString()) 
            : BigDecimal.ZERO;
            
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal seatMod = BigDecimal.ZERO;

        // Seat Logic
        if (ticket.getAssignedSeat() != null && ticket.getAssignedSeat().getId() != null) {
            // If the seat object is a proxy, fetch it to get the price modifier
            Seat seat = seatService.getById(ticket.getAssignedSeat().getId());
            
            if (seat.getSeatPriceModifier() != null) {
                seatMod = new BigDecimal(seat.getSeatPriceModifier().toString());
            }

            if (seat.getChairType() == ChairType.KIDS_CHAIR) {
                // 50% discount for Kids
                discount = base.multiply(new BigDecimal("0.50")); 
            }
        }

        // Meal Logic
        BigDecimal mealsTotal = BigDecimal.ZERO;
        if (ticket.getSelectedMeals() != null && !ticket.getSelectedMeals().isEmpty()) {
            ticket.setHasMealPlan(true);
            for (MealPlan mp : ticket.getSelectedMeals()) {
                MealPlan dbMeal = mealPlanService.findById(mp.getId());
                if (dbMeal.getMealPrice() != null) {
                    mealsTotal = mealsTotal.add(new BigDecimal(dbMeal.getMealPrice().toString()));
                }
            }
        } else {
            ticket.setHasMealPlan(false);
        }

        // Set the values to the Ticket
        ticket.setBasePrice(base);
        ticket.setDiscountPrice(discount);
        ticket.setSeatPriceModifier(seatMod);
        
        // Math: (base - discount) + seatMod + mealsTotal
        BigDecimal total = base.subtract(discount).add(seatMod).add(mealsTotal);
        ticket.setTotalPrice(total);
    }
@PostConstruct
    public void init() {
        // Register the auto-cancel logic into the generic scheduler
        schedulingService.registerNightlyTask(this::autoCancelExpiredTickets);
    }

@Transactional
    public void autoCancelExpiredTickets() {
        java.time.LocalDate now = java.time.LocalDate.now();
        
        // This will now work with the Repository update above
        List<Ticket> expiredTickets = ticketRepository.findAllPendingByDateBefore(now);

        for (Ticket ticket : expiredTickets) {
            try {
                // Update state
                ticket.setTicketStatus(TicketStatus.CANCELLED);
                ticket.setApprovalStatus(GenericStatus.CANCELLED);
                
                inventoryService.release(ticket.getTour().getId(), 1, List.of(ticket));
                
                ticketRepository.save(ticket);
                
                // Send notification via your dedicated function
                notificationService.sendTicketAutoCancellationNotification(ticket);
                
                log.info("System auto-cancelled expired ticket: {}", ticket.getTicketNumber());
            } catch (Exception e) {
                log.error("Failed to auto-cancel ticket {}: {}", ticket.getTicketNumber(), e.getMessage());
            }
        }
    }

}
