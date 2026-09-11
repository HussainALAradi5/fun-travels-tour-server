package com.server.server.services.Account;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.GenericStatus;
import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.enums.Payment.PaymentStatus;
import com.server.server.enums.TransactionType;
import com.server.server.enums.tourmanagement.SeatStatus;
import com.server.server.enums.tourmanagement.TicketStatus;
import com.server.server.models.Account;
import com.server.server.models.Payment;
import com.server.server.models.tourmanagement.Ticket;
import com.server.server.models.tourmanagement.TourReservation;
import com.server.server.repositories.PaymentRepository;
import com.server.server.repositories.tourmanagement.TourReservationRepository;
import com.server.server.services.TransactionService;
import com.server.server.services.tourmanagement.SeatService;
import com.server.server.services.tourmanagement.TourService;

@Service
public class PaymentProcessingService {

    @Autowired
    private AccountService accountService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private TourReservationRepository reservationRepository;
    @Autowired
    private TourService tourService;
    @Autowired
    private SeatService seatService;

    @Transactional
    public TourReservation processCheckout(TourReservation reservation, PaymentMethod chosenMethod, String externalTransactionId) {
        
        BigDecimal amountDue = reservation.getTotalPrice();
        boolean isPaid = false;

        // SCENARIO 1: Internal Wallet
        if (chosenMethod == PaymentMethod.WALLET) {
            Account userAccount = accountService.getAccountByUserId(reservation.getUser().getId());
            transactionService.debitAccount(userAccount, amountDue, TransactionType.PAYMENT, "Paid for Tour: " + reservation.getReservationNumber(), reservation);
            isPaid = true;
        } 
        // SCENARIO 2: Stripe / PayPal
        else if (chosenMethod == PaymentMethod.CREDIT_CARD || chosenMethod == PaymentMethod.PAYPAL) {
            Payment payment = Payment.builder()
                    .method(chosenMethod)
                    .transactionId(externalTransactionId)
                    .amount(amountDue)
                    .status(PaymentStatus.COMPLETED)
                    .reservation(reservation)
                    .build();
            paymentRepository.save(payment);
            isPaid = true;
        }
        // SCENARIO 3: Bank Transfer / Cash
        else if (chosenMethod == PaymentMethod.BANK_TRANSFER || chosenMethod == PaymentMethod.CASH_AT_OFFICE) {
            reservation.setStatus(GenericStatus.PENDING);
        }

        // --- NEW BUSINESS RULE: APPROVE TICKETS & LOCK SEATS ONLY IF PAID ---
        if (isPaid && reservation.getStatus() == GenericStatus.PENDING) {
            reservation.setStatus(GenericStatus.CONFIRMED);
            
            // 1. Decrease available slots on the tour
            tourService.restoreInventory(reservation.getTour().getId(), -reservation.getRequestedSlots());

            // 2. Approve all tickets and lock their specific seats
            if (reservation.getTickets() != null) {
                for (Ticket ticket : reservation.getTickets()) {
                    ticket.setTicketStatus(TicketStatus.CONFIRMED);
                    ticket.setApprovalStatus(GenericStatus.APPROVED);
                    
                    if (ticket.getAssignedSeat() != null) {
                        seatService.updateStatus(ticket.getAssignedSeat().getId(), SeatStatus.BOOKED);
                    }
                }
            }
        }

        return reservationRepository.save(reservation);
    }
}
