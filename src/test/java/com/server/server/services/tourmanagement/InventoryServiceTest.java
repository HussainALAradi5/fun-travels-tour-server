package com.server.server.services.tourmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.exceptions.WorkflowException;
import com.server.server.models.tourmanagement.Seat;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.models.tourmanagement.Transportation;
import com.server.server.repositories.tourmanagement.SeatRepository;
import com.server.server.repositories.tourmanagement.TicketRepository;
import com.server.server.repositories.tourmanagement.TourRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceTest {

    @Mock
    private TourRepository tourRepository;
    @Mock
    private SeatRepository seatRepository;
    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private com.server.server.services.common.SystemSchedulingService schedulingService;
    @InjectMocks
    private InventoryService inventory;

    private Tour tour;
    private Seat s1;
    private Seat s2;
    private Seat s3;

    @BeforeEach
    void setUp() {
        Transportation bus = new Transportation();
        bus.setId(20);
        tour = new Tour();
        tour.setId(5);
        tour.setStatus(GenericStatus.ACTIVE);
        tour.setMaxCapacity(3);
        tour.setAvailableSlots(3);
        tour.setTransportation(bus);
        s1 = seat(1, bus, SeatStatus.AVAILABLE);
        s2 = seat(2, bus, SeatStatus.MAINTENANCE);
        s3 = seat(3, bus, SeatStatus.BOOKED); // legacy global status no longer blocks other tours
        when(tourRepository.findByIdWithLock(5)).thenReturn(Optional.of(tour));
        when(tourRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(seatRepository.findByTransportation_IdAndStatusNotOrderByIdAsc(20, SeatStatus.MAINTENANCE))
                .thenReturn(List.of(s1, s3));
        when(seatRepository.findById(1)).thenReturn(Optional.of(s1));
        when(seatRepository.findById(2)).thenReturn(Optional.of(s2));
        when(seatRepository.findById(3)).thenReturn(Optional.of(s3));
    }

    @Test
    void seatTakenOnThisTourIsSkippedWhenAssigningAutomatically() {
        when(ticketRepository.findTakenSeatIds(5)).thenReturn(List.of(1));
        Ticket ticket = new Ticket();

        inventory.reserve(5, 1, List.of(ticket));

        assertThat(ticket.getAssignedSeat().getId()).isEqualTo(3);
        assertThat(tour.getAvailableSlots()).isEqualTo(2);
    }

    @Test
    void seatsSoldOnAnotherTourOfTheSameVehicleStayAvailable() {
        when(ticketRepository.findTakenSeatIds(5)).thenReturn(List.of());
        Ticket ticket = withSeat(3);

        inventory.reserve(5, 1, List.of(ticket));

        assertThat(ticket.getAssignedSeat().getId()).isEqualTo(3);
    }

    @Test
    void maintenanceAndTakenSeatsCannotBeChosen() {
        when(ticketRepository.findTakenSeatIds(5)).thenReturn(List.of(1));
        assertThatThrownBy(() -> inventory.reserve(5, 1, List.of(withSeat(2))))
                .isInstanceOf(WorkflowException.class).hasMessageContaining("no longer available");
        assertThatThrownBy(() -> inventory.reserve(5, 1, List.of(withSeat(1))))
                .isInstanceOf(WorkflowException.class);
    }

    @Test
    void explicitSeatIsNotHandedToAnotherTravelerAutomatically() {
        when(ticketRepository.findTakenSeatIds(5)).thenReturn(List.of());
        Ticket automatic = new Ticket();
        Ticket chosen = withSeat(1);

        inventory.reserve(5, 2, List.of(automatic, chosen));

        assertThat(automatic.getAssignedSeat().getId()).isEqualTo(3);
        assertThat(chosen.getAssignedSeat().getId()).isEqualTo(1);
    }

    @Test
    void notEnoughFreeSeatsFailsTheWholeHold() {
        when(ticketRepository.findTakenSeatIds(5)).thenReturn(List.of(1, 3));
        assertThatThrownBy(() -> inventory.reserve(5, 1, List.of(new Ticket())))
                .isInstanceOf(WorkflowException.class).hasMessageContaining("not enough transportation seats");
        assertThat(tour.getAvailableSlots()).isEqualTo(3);
    }

    @Test
    void releaseNeverExceedsCapacity() {
        inventory.release(5, 5, List.of());
        assertThat(tour.getAvailableSlots()).isEqualTo(3);
    }

    private static Seat seat(int id, Transportation bus, SeatStatus status) {
        Seat seat = new Seat();
        seat.setId(id);
        seat.setSeatCode("S" + id);
        seat.setTransportation(bus);
        seat.setStatus(status);
        return seat;
    }

    private static Ticket withSeat(int seatId) {
        Seat ref = new Seat();
        ref.setId(seatId);
        Ticket ticket = new Ticket();
        ticket.setAssignedSeat(ref);
        return ticket;
    }

    @Test
    void bookingCountsPlacesFromTicketsNotTheStoredCounter() {
        tour.setAvailableSlots(0); // drifted counter says sold out
        when(ticketRepository.countByTour_IdAndTicketStatusNot(5, com.server.server.enums.tourmanagement.TicketStatus.CANCELLED)).thenReturn(1L);
        inventory.reserve(5, 1, List.of(new Ticket()));
        assertThat(tour.getAvailableSlots()).isEqualTo(1); // 3 capacity - 1 held - 1 new
    }

    @Test
    void bookingIsRefusedWhenTicketsFillTheTourEvenIfTheCounterSaysOtherwise() {
        tour.setAvailableSlots(3); // drifted counter says empty
        when(ticketRepository.countByTour_IdAndTicketStatusNot(5, com.server.server.enums.tourmanagement.TicketStatus.CANCELLED)).thenReturn(3L);
        assertThatThrownBy(() -> inventory.reserve(5, 1, List.of(new Ticket())))
                .isInstanceOf(WorkflowException.class).hasMessageContaining("not enough places");
    }

    @Test
    void nightlyReconciliationFixesDriftedTours() {
        tour.setAvailableSlots(3);
        when(tourRepository.findByStatus(GenericStatus.ACTIVE)).thenReturn(List.of(tour));
        when(ticketRepository.countByTour_IdAndTicketStatusNot(5, com.server.server.enums.tourmanagement.TicketStatus.CANCELLED)).thenReturn(2L);
        assertThat(inventory.reconcileActiveTours()).isEqualTo(1);
        assertThat(tour.getAvailableSlots()).isEqualTo(1);
        assertThat(inventory.reconcileActiveTours()).isZero();
    }
}
