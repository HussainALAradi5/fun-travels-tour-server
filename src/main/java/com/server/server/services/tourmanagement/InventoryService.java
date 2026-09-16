package com.server.server.services.tourmanagement;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.enums.tourmanagement.TransportationStatus;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.repositories.tourmanagement.SeatRepository;
import com.server.server.repositories.tourmanagement.TourRepository;
import com.server.server.repositories.tourmanagement.TransportationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InventoryService {
    private final TourRepository tourRepository;
    private final SeatRepository seatRepository;
    private final TransportationRepository transportationRepository;

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
        synchronizeTransportation(tour);
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
        synchronizeTransportationForTickets(tickets);
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
        synchronizeTransportation(tour);
        tourRepository.save(tour);
    }

    @Transactional
    public Seat changeSeat(Tour tour, Ticket ticket, Integer newSeatId) {
        if (tour == null || ticket == null || newSeatId == null) {
            throw new WorkflowException("A tour, ticket, and new seat are required.");
        }
        Seat currentSeat = ticket.getAssignedSeat() == null ? null : lockSeat(ticket.getAssignedSeat().getId());
        if (currentSeat != null && Objects.equals(currentSeat.getId(), newSeatId)) return currentSeat;

        Seat newSeat = lockSeat(newSeatId);
        if (newSeat.getStatus() != SeatStatus.AVAILABLE) {
            throw new WorkflowException("SEAT_NOT_AVAILABLE",
                    "Seat " + newSeat.getSeatCode() + " is no longer available. Please choose another seat.");
        }
        if (tour.getTransportation() == null || newSeat.getTransportation() == null
                || !Objects.equals(tour.getTransportation().getId(), newSeat.getTransportation().getId())) {
            throw new WorkflowException("INVALID_SEAT_FOR_TOUR",
                    "The selected seat does not belong to this tour's transportation.");
        }

        newSeat.setStatus(ticket.isPaid() ? SeatStatus.BOOKED : SeatStatus.RESERVED);
        seatRepository.save(newSeat);
        if (currentSeat != null) {
            currentSeat.setStatus(SeatStatus.AVAILABLE);
            seatRepository.save(currentSeat);
        }
        synchronizeTransportation(tour);
        return newSeat;
    }

    private void validateAndHoldSeats(Tour tour, List<Ticket> tickets) {
        Set<Integer> selected = new HashSet<>();
        List<Seat> automaticallyAvailable = tour.getTransportation() == null
                ? List.of()
                : seatRepository.findAvailableByTransportationIdWithLock(tour.getTransportation().getId());
        int automaticIndex = 0;
        for (Ticket ticket : safeTickets(tickets)) {
            Integer seatId = ticket.getAssignedSeat() == null ? null : ticket.getAssignedSeat().getId();
            if (seatId == null && tour.getTransportation() != null) {
                while (automaticIndex < automaticallyAvailable.size()
                        && selected.contains(automaticallyAvailable.get(automaticIndex).getId())) {
                    automaticIndex++;
                }
                if (automaticIndex >= automaticallyAvailable.size()) {
                    throw new WorkflowException("INSUFFICIENT_TRANSPORTATION_SEATS",
                            "There are not enough transportation seats available for this booking.");
                }
                seatId = automaticallyAvailable.get(automaticIndex++).getId();
            }
            if (seatId == null) continue;
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

    private void synchronizeTransportationForTickets(List<Ticket> tickets) {
        safeTickets(tickets).stream()
                .map(Ticket::getTour)
                .filter(Objects::nonNull)
                .filter(tour -> tour.getTransportation() != null)
                .map(Tour::getId)
                .filter(Objects::nonNull)
                .distinct()
                .map(this::lockTour)
                .forEach(this::synchronizeTransportation);
    }

    private void synchronizeTransportation(Tour tour) {
        if (tour.getTransportation() == null || tour.getTransportation().getId() == null) return;
        Integer transportationId = tour.getTransportation().getId();
        var transportation = transportationRepository.findByIdWithLock(transportationId)
                .orElseThrow(() -> new WorkflowException("Transportation not found."));
        int availableSeats = Math.toIntExact(
                seatRepository.countByTransportation_IdAndStatus(transportationId, SeatStatus.AVAILABLE));
        transportation.setRemainingSeats(availableSeats);
        transportation.setCalculatedAvailable(availableSeats);
        if (transportation.getUnitStatus() != TransportationStatus.MAINTENANCE
                && transportation.getUnitStatus() != TransportationStatus.INACTIVE) {
            if (availableSeats == 0) {
                transportation.setUnitStatus(TransportationStatus.FULL);
            } else if (availableSeats < transportation.getTotalCapacity()) {
                transportation.setUnitStatus(TransportationStatus.PARTIAL);
            } else {
                transportation.setUnitStatus(TransportationStatus.AVAILABLE);
            }
        }
        transportationRepository.save(transportation);
        tour.setTransportation(transportation);
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
