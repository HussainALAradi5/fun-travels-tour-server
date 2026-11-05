package com.server.server.services.tourmanagement;

import com.server.server.enums.tourmanagement.ChairType;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.exceptions.common.WorkflowException;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.repositories.tourmanagement.SeatRepository;
import com.server.server.repositories.tourmanagement.TicketRepository;
import com.server.server.repositories.tourmanagement.TourRepository;

import lombok.RequiredArgsConstructor;
import com.server.server.enums.tourmanagement.TicketStatus;
import jakarta.annotation.PostConstruct;
import com.server.server.services.common.SystemSchedulingService;

/**
 * Tour places and seat holds.
 *
 * <p>Places: {@code Tour.availableSlots} drops when a booking is held and rises when it is
 * released. The tour row is locked for every change, so bookings for one tour are serialized.
 *
 * <p>Seats: a vehicle can serve several tours on different dates, so a seat is taken
 * <em>per tour</em> when an active (not cancelled) ticket of that tour holds it. The seat's own
 * status only describes its physical condition: {@code MAINTENANCE} seats are never sold.
 * Cancelling a ticket therefore frees its seat for that tour without touching other tours.
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final TourRepository tourRepository;
    private final SeatRepository seatRepository;
    private final TicketRepository ticketRepository;
    private final SystemSchedulingService schedulingService;

    @PostConstruct
    void registerReconciliation() {
        schedulingService.registerNightlyTask(this::reconcileActiveTours);
    }

    /** Holds places and seats. Tickets without a chosen seat get the lowest free seat. */
    @Transactional
    public Tour reserve(Integer tourId, int quantity, List<Ticket> tickets) {
        Tour tour = lockTour(tourId);
        requirePositive(quantity);
        if (tour.getStatus() != GenericStatus.ACTIVE) {
            throw new WorkflowException("TOUR_NOT_BOOKABLE", "This tour is not available for booking.");
        }
        // Counted from tickets, not trusted from the stored counter, so any drift is corrected at each booking.
        int available = placesLeft(tour);
        if (available < quantity) {
            throw new WorkflowException("INSUFFICIENT_CAPACITY", "There are not enough places available for this booking.");
        }
        assignSeats(tour, tickets);
        tour.setAvailableSlots(available - quantity);
        return tourRepository.save(tour);
    }

    /** Nightly: sets every bookable tour's available places from its tickets. */
    @Transactional
    public int reconcileActiveTours() {
        int corrected = 0;
        for (Tour tour : tourRepository.findByStatus(GenericStatus.ACTIVE)) {
            int actual = placesLeft(tour);
            if (!Objects.equals(tour.getAvailableSlots(), actual)) {
                tour.setAvailableSlots(actual);
                tourRepository.save(tour);
                corrected++;
            }
        }
        return corrected;
    }

    /** Capacity minus the tour's tickets that are not cancelled (saved tickets only). */
    private int placesLeft(Tour tour) {
        int capacity = Objects.requireNonNullElse(tour.getMaxCapacity(), 0);
        long held = ticketRepository.countByTour_IdAndTicketStatusNot(tour.getId(), TicketStatus.CANCELLED);
        return (int) Math.max(0, capacity - held);
    }

    /** Returns places to the tour. Seats free themselves once their tickets are cancelled. */
    @Transactional
    public void release(Integer tourId, int quantity, List<Ticket> tickets) {
        Tour tour = lockTour(tourId);
        requirePositive(quantity);
        int available = Objects.requireNonNullElse(tour.getAvailableSlots(), 0);
        int capacity = Objects.requireNonNullElse(tour.getMaxCapacity(), 0);
        tour.setAvailableSlots(Math.min(capacity, available + quantity));
        tourRepository.save(tour);
    }

    /** Ids of seats held by active tickets of the tour. */
    @Transactional(readOnly = true)
    public Set<Integer> takenSeatIds(Integer tourId) {
        return new HashSet<>(ticketRepository.findTakenSeatIds(tourId));
    }

    /** True when a seat is held by an active ticket of any tour that has not finished. */
    @Transactional(readOnly = true)
    public boolean isSeatInUse(Integer seatId) {
        return ticketRepository.existsActiveTicketForSeat(seatId);
    }

    private void assignSeats(Tour tour, List<Ticket> tickets) {
        List<Ticket> safe = tickets == null ? List.of() : tickets;
        if (tour.getTransportation() == null) {
            if (safe.stream().anyMatch(ticket -> ticket.getAssignedSeat() != null)) {
                throw new WorkflowException("INVALID_SEAT_FOR_TOUR", "This tour has no transportation seats to choose.");
            }
            return;
        }
        Integer transportationId = tour.getTransportation().getId();
        Set<Integer> taken = takenSeatIds(tour.getId());
        Set<Integer> chosen = new HashSet<>();
        for (Ticket ticket : safe) {
            if (ticket.getAssignedSeat() == null || ticket.getAssignedSeat().getId() == null) {
                continue;
            }
            Seat seat = seatRepository.findById(ticket.getAssignedSeat().getId())
                    .orElseThrow(() -> new WorkflowException("SEAT_NOT_FOUND", "Seat not found."));
            if (seat.getTransportation() == null || !Objects.equals(seat.getTransportation().getId(), transportationId)) {
                throw new WorkflowException("INVALID_SEAT_FOR_TOUR", "The selected seat does not belong to this tour's transportation.");
            }
            if (!chosen.add(seat.getId())) {
                throw new WorkflowException("DUPLICATE_SEAT", "Each traveler must have a different seat.");
            }
            if (seat.getStatus() == SeatStatus.MAINTENANCE || taken.contains(seat.getId())) {
                throw new WorkflowException("SEAT_NOT_AVAILABLE",
                        "Seat " + seat.getSeatCode() + " is no longer available. Please choose another seat.");
            }
            ticket.setAssignedSeat(seat);
        }
        // Travelers who did not choose a seat get standard seats only: a kids chair would discount the ticket,
        // a premium recliner would charge extra, and accessible seats are kept for those who ask for them.
        List<Seat> free = seatRepository.findByTransportation_IdAndStatusNotOrderByIdAsc(transportationId, SeatStatus.MAINTENANCE)
                .stream()
                .filter(seat -> !taken.contains(seat.getId()) && !chosen.contains(seat.getId()))
                .filter(seat -> seat.getChairType() == null || seat.getChairType() == ChairType.STANDARD)
                .toList();
        int next = 0;
        for (Ticket ticket : safe) {
            if (ticket.getAssignedSeat() != null && ticket.getAssignedSeat().getId() != null) {
                continue;
            }
            if (next >= free.size()) {
                throw new WorkflowException("SEAT_CHOICE_REQUIRED",
                        "No standard seats are left on this tour. Please choose a seat for each traveler.");
            }
            ticket.setAssignedSeat(free.get(next++));
        }
    }

    private Tour lockTour(Integer tourId) {
        return tourRepository.findByIdWithLock(tourId)
                .orElseThrow(() -> new WorkflowException("Tour not found."));
    }

    private static void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new WorkflowException("Inventory quantity must be greater than zero.");
        }
    }
}
