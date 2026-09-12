package com.server.server.services.tourmanagement;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.repositories.tourmanagement.SeatRepository;
import com.server.server.repositories.tourmanagement.TourRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final TourRepository tourRepository;
    private final SeatRepository seatRepository;

    @Transactional
    public Tour reserve(Integer tourId, int quantity, List<Ticket> tickets) {
        Tour tour = lockTour(tourId);
        requirePositive(quantity);
        if (tour.getStatus() != GenericStatus.ACTIVE) {
            throw new WorkflowException("TOUR_NOT_BOOKABLE", "This tour is not available for booking.");
        }
        int available = Objects.requireNonNullElse(tour.getAvailableSlots(), 0);
        if (available < quantity) throw new WorkflowException("INSUFFICIENT_CAPACITY",
                "There are not enough places available for this booking.");
        validateAndHoldSeats(tour, tickets);
        tour.setAvailableSlots(available - quantity);
        return tourRepository.save(tour);
    }

    @Transactional
    public void confirmSeats(List<Ticket> tickets) {
        for (Ticket ticket : safeTickets(tickets)) {
            if (ticket.getAssignedSeat() != null) {
                Seat seat = lockSeat(ticket.getAssignedSeat().getId());
                if (seat.getStatus() != SeatStatus.RESERVED && seat.getStatus() != SeatStatus.BOOKED) {
                    throw new WorkflowException("Seat " + seat.getSeatCode() + " is not held by this booking.");
                }
                seat.setStatus(SeatStatus.BOOKED);
                seatRepository.save(seat);
            }
        }
    }

    @Transactional
    public void release(Integer tourId, int quantity, List<Ticket> tickets) {
        Tour tour = lockTour(tourId);
        requirePositive(quantity);
        int available = Objects.requireNonNullElse(tour.getAvailableSlots(), 0);
        int capacity = Objects.requireNonNullElse(tour.getMaxCapacity(), 0);
        tour.setAvailableSlots(Math.min(capacity, available + quantity));
        for (Ticket ticket : safeTickets(tickets)) {
            if (ticket.getAssignedSeat() != null) {
                Seat seat = lockSeat(ticket.getAssignedSeat().getId());
                seat.setStatus(SeatStatus.AVAILABLE);
                seatRepository.save(seat);
            }
        }
        tourRepository.save(tour);
    }

    private void validateAndHoldSeats(Tour tour, List<Ticket> tickets) {
        Set<Integer> selected = new HashSet<>();
        for (Ticket ticket : safeTickets(tickets)) {
            if (ticket.getAssignedSeat() == null || ticket.getAssignedSeat().getId() == null) continue;
            Integer seatId = ticket.getAssignedSeat().getId();
            if (!selected.add(seatId)) throw new WorkflowException("DUPLICATE_SEAT",
                    "Each traveler must have a different seat.");
            Seat seat = lockSeat(seatId);
            if (seat.getStatus() != SeatStatus.AVAILABLE) {
                throw new WorkflowException("SEAT_NOT_AVAILABLE",
                        "Seat " + seat.getSeatCode() + " is no longer available. Please choose another seat.");
            }
            if (tour.getTransportation() == null || seat.getTransportation() == null
                    || !Objects.equals(tour.getTransportation().getId(), seat.getTransportation().getId())) {
                throw new WorkflowException("INVALID_SEAT_FOR_TOUR",
                        "The selected seat does not belong to this tour's transportation.");
            }
            seat.setStatus(SeatStatus.RESERVED);
            seatRepository.save(seat);
            ticket.setAssignedSeat(seat);
        }
    }

    private Tour lockTour(Integer tourId) {
        return tourRepository.findByIdWithLock(tourId)
                .orElseThrow(() -> new WorkflowException("Tour not found."));
    }

    private Seat lockSeat(Integer seatId) {
        return seatRepository.findByIdWithLock(seatId)
                .orElseThrow(() -> new WorkflowException("Seat not found."));
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) throw new WorkflowException("Inventory quantity must be greater than zero.");
    }

    private List<Ticket> safeTickets(List<Ticket> tickets) {
        return tickets == null ? List.of() : tickets;
    }
}
