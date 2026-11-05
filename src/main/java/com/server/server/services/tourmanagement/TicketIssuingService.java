package com.server.server.services.tourmanagement;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.server.server.constants.tourmanagement.BookingRules;
import com.server.server.constants.common.CodePrefixes;
import com.server.server.constants.errors.WorkflowErrorCodes;
import com.server.server.dto.common.EntityReference;
import com.server.server.dto.tourmanagement.TicketSelectionRequest;
import com.server.server.enums.common.GenericStatus;
import com.server.server.exceptions.common.WorkflowException;
import com.server.server.models.tourmanagement.MealPlan;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.models.user.User;
import com.server.server.services.common.CodeGenerationService;
import com.server.server.utilities.codes.CodeGenerator;
import com.server.server.utilities.pricing.PricingCalculator;
import com.server.server.utilities.pricing.PricingPolicy;
import com.server.server.utilities.pricing.PricingCalculator.TicketPrice;

import lombok.RequiredArgsConstructor;

/**
 * Builds priced tickets with numbers and scan codes. Prices always come from the stored
 * tour, seat, and meal records, never from the request.
 */
@Service
@RequiredArgsConstructor
public class TicketIssuingService {

    private final SeatService seatService;
    private final MealPlanService mealPlanService;
    private final CodeGenerationService codeGenerationService;

    /** Creates one unsaved ticket per traveler selection. */
    public List<Ticket> issue(Tour tour, User customer, TourReservation reservation,
            List<TicketSelectionRequest> selections) {
        List<Ticket> tickets = new ArrayList<>();
        for (TicketSelectionRequest selection : selections) {
            Ticket ticket = new Ticket();
            if (selection.assignedSeat() != null && selection.assignedSeat().id() != null) {
                Seat seat = new Seat();
                seat.setId(selection.assignedSeat().id());
                ticket.setAssignedSeat(seat);
            }
            ticket.setSelectedMeals(resolveMeals(tour, selection.selectedMeals()));
            tickets.add(prepare(ticket, tour, customer, reservation));
        }
        return tickets;
    }

    /** Fills identity, codes, relations, and price on a ticket. */
    public Ticket prepare(Ticket ticket, Tour tour, User customer, TourReservation reservation) {
        String ticketNumber = CodeGenerator.generate(CodePrefixes.TICKET);
        ticket.setTour(tour);
        ticket.setCustomer(customer);
        ticket.setReservation(reservation);
        ticket.setTicketNumber(ticketNumber);
        ticket.setBookingDate(LocalDateTime.now());
        try {
            String qrData = String.format("TICKET:%s|TOUR:%d|CUSTOMER:%d", ticketNumber, tour.getId(), customer.getId());
            ticket.setQrCode(codeGenerationService.generateQRCode(qrData, BookingRules.QR_SIZE, BookingRules.QR_SIZE));
            ticket.setBarcode(codeGenerationService.generateBarcode(ticketNumber,
                    BookingRules.BARCODE_WIDTH, BookingRules.BARCODE_HEIGHT));
        } catch (Exception e) {
            throw new WorkflowException(WorkflowErrorCodes.TICKET_CODE_FAILED, "Ticket codes could not be generated.");
        }
        applyPricing(ticket, tour);
        return ticket;
    }

    /** Recalculates price from the stored tour, seat, and meals. */
    public void applyPricing(Ticket ticket, Tour tour) {
        Seat seat = ticket.getAssignedSeat() != null && ticket.getAssignedSeat().getId() != null
                ? seatService.getById(ticket.getAssignedSeat().getId())
                : null;
        List<MealPlan> meals = ticket.getSelectedMeals() == null ? List.of() : ticket.getSelectedMeals();
        TicketPrice price = PricingCalculator.ticketPrice(
                PricingCalculator.money(tour.getTotalPrice()),
                seat == null ? null : seat.getChairType(),
                seat == null ? BigDecimal.ZERO : PricingCalculator.money(seat.getSeatPriceModifier()),
                meals.stream().map(meal -> PricingCalculator.money(meal.getMealPrice())).toList(),
                PricingPolicy.of(tour.getAgency()));
        if (seat != null) {
            ticket.setAssignedSeat(seat);
        }
        ticket.setHasMealPlan(!meals.isEmpty());
        ticket.setBasePrice(price.base());
        ticket.setDiscountPrice(price.discount());
        ticket.setSeatPriceModifier(price.seatModifier());
        ticket.setTotalPrice(price.total());
    }

    private List<MealPlan> resolveMeals(Tour tour, List<EntityReference> references) {
        List<MealPlan> meals = new ArrayList<>();
        if (references == null) {
            return meals;
        }
        for (EntityReference reference : references) {
            if (reference == null || reference.id() == null) {
                continue;
            }
            MealPlan meal = mealPlanService.findById(reference.id());
            if (meals.stream().anyMatch(selected -> Objects.equals(selected.getId(), meal.getId()))) {
                throw new WorkflowException(WorkflowErrorCodes.DUPLICATE_MEAL, "Meal '" + meal.getMealName() + "' was selected twice.");
            }
            if (meal.getStatus() != GenericStatus.ACTIVE) {
                throw new WorkflowException(WorkflowErrorCodes.MEAL_INACTIVE, "Meal '" + meal.getMealName() + "' is no longer available.");
            }
            boolean offered = tour.getAvailableMeals().stream()
                    .anyMatch(available -> Objects.equals(available.getId(), meal.getId()));
            if (!offered) {
                throw new WorkflowException(WorkflowErrorCodes.MEAL_NOT_OFFERED, "Meal '" + meal.getMealName() + "' is not offered on this tour.");
            }
            meals.add(meal);
        }
        return meals;
    }
}
