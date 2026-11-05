package com.server.server.services.tourmanagement;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.common.GenericStatus;
import com.server.server.enums.payment.TransactionType;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.models.payment.Account;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.services.payment.AccountService;
import com.server.server.services.payment.TransactionService;
import com.server.server.utilities.pricing.PricingCalculator;

import lombok.RequiredArgsConstructor;

/**
 * Shared steps for every way a booking ends early: customer cancellation, hold expiry,
 * staff rejection, and agency tour cancellation. Only tickets that are still active are
 * released or refunded, so a ticket cancelled earlier is never refunded or freed twice.
 */
@Service
@RequiredArgsConstructor
public class BookingLifecycleService {

    private final InventoryService inventoryService;
    private final AccountService accountService;
    private final TransactionService transactionService;

    public static List<Ticket> activeTickets(TourReservation reservation) {
        List<Ticket> tickets = reservation.getTickets() == null ? List.of() : reservation.getTickets();
        return tickets.stream().filter(ticket -> ticket.getTicketStatus() != TicketStatus.CANCELLED).toList();
    }

    /** Amount still covered by the reservation; legacy reservations without tickets use their total. */
    public static BigDecimal activeTotal(TourReservation reservation) {
        if (reservation.getTickets() == null || reservation.getTickets().isEmpty()) {
            return PricingCalculator.money(reservation.getTotalPrice());
        }
        return PricingCalculator.money(activeTickets(reservation).stream()
                .map(Ticket::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Returns held or sold places to inventory and marks the active tickets cancelled. */
    @Transactional
    public void releaseAndCancelTickets(TourReservation reservation, GenericStatus ticketApprovalStatus) {
        List<Ticket> active = activeTickets(reservation);
        boolean legacyWithoutTickets = reservation.getTickets() == null || reservation.getTickets().isEmpty();
        int units = legacyWithoutTickets ? Objects.requireNonNullElse(reservation.getRequestedSlots(), 0) : active.size();
        if (units > 0) {
            inventoryService.release(reservation.getTour().getId(), units, active);
        }
        active.forEach(ticket -> {
            ticket.setTicketStatus(TicketStatus.CANCELLED);
            ticket.setApprovalStatus(ticketApprovalStatus);
        });
    }

    /** Credits the customer's wallet when the amount is positive. */
    @Transactional
    public void refundToWallet(TourReservation reservation, BigDecimal amount, TransactionType type, String reason) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }
        Account account = accountService.getAccountByUserId(reservation.getUser().getId());
        transactionService.creditAccount(account, amount, type, reason, reservation);
    }
}
