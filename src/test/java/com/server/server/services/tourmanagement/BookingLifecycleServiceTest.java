package com.server.server.services.tourmanagement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.payment.TransactionType;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.Tour;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.services.payment.AccountService;
import com.server.server.services.payment.TransactionService;

@ExtendWith(MockitoExtension.class)
class BookingLifecycleServiceTest {

    @Mock
    private InventoryService inventoryService;
    @Mock
    private AccountService accountService;
    @Mock
    private TransactionService transactionService;
    @InjectMocks
    private BookingLifecycleService service;

    @Test
    void releasesOnlyTicketsThatAreStillActive() {
        TourReservation reservation = reservation(ticket("100", TicketStatus.CONFIRMED),
                ticket("80", TicketStatus.CANCELLED), ticket("60", TicketStatus.CONFIRMED));

        service.releaseAndCancelTickets(reservation, GenericStatus.CANCELLED);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Ticket>> released = ArgumentCaptor.forClass(List.class);
        verify(inventoryService).release(eq(7), eq(2), released.capture());
        assertThat(released.getValue()).hasSize(2)
                .allMatch(ticket -> ticket.getTotalPrice().compareTo(new BigDecimal("80")) != 0);
        assertThat(reservation.getTickets()).allMatch(ticket -> ticket.getTicketStatus() == TicketStatus.CANCELLED);
    }

    @Test
    void activeTotalIgnoresTicketsRefundedEarlier() {
        TourReservation reservation = reservation(ticket("100", TicketStatus.CONFIRMED),
                ticket("80", TicketStatus.CANCELLED));
        assertThat(BookingLifecycleService.activeTotal(reservation)).isEqualByComparingTo("100");
    }

    @Test
    void legacyReservationWithoutTicketsReleasesRequestedSlots() {
        TourReservation reservation = reservation();
        reservation.setRequestedSlots(3);
        reservation.setTotalPrice(new BigDecimal("300"));

        service.releaseAndCancelTickets(reservation, GenericStatus.CANCELLED);

        verify(inventoryService).release(eq(7), eq(3), any());
        assertThat(BookingLifecycleService.activeTotal(reservation)).isEqualByComparingTo("300");
    }

    @Test
    void zeroRefundDoesNotTouchTheWallet() {
        service.refundToWallet(reservation(), BigDecimal.ZERO, TransactionType.REFUND, "none");
        verify(transactionService, never()).creditAccount(any(), any(), any(), any(), any());
        verify(inventoryService, never()).release(anyInt(), anyInt(), any());
    }

    private static TourReservation reservation(Ticket... tickets) {
        Tour tour = new Tour();
        tour.setId(7);
        TourReservation reservation = new TourReservation();
        reservation.setTour(tour);
        reservation.setTickets(new ArrayList<>(List.of(tickets)));
        return reservation;
    }

    private static Ticket ticket(String total, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setTotalPrice(new BigDecimal(total));
        ticket.setTicketStatus(status);
        return ticket;
    }
}
