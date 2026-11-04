package com.server.server.services.payment;

import com.server.server.constants.BookingRules;

import com.server.server.constants.CodePrefixes;

import com.server.server.utilities.CodeGenerator;

import com.server.server.exceptions.WorkflowException;

import com.server.server.services.auth.AccessPolicyService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.payment.PaymentMethod;
import com.server.server.enums.payment.PaymentStatus;
import com.server.server.enums.payment.TransactionType;
import com.server.server.models.payment.Account;
import com.server.server.models.payment.Payment;
import com.server.server.models.payment.Transaction;
import com.server.server.models.user.User;
import com.server.server.repositories.payment.PaymentRepository;

import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WalletTopUpService {

    private final AccountService accountService;
    
    private final TransactionService transactionService;
    
    private final PaymentRepository paymentRepository;
    
    private final AccessPolicyService accessPolicy;

    @Transactional
    public Transaction processWalletTopUp(BigDecimal amount, PaymentMethod method, String stripeToken) {
        
        // 1. Validate Amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        if (method != PaymentMethod.CREDIT_CARD) {
            throw new IllegalArgumentException("Wallet top-ups are only supported by card.");
        }
        if (stripeToken == null || stripeToken.isBlank()) {
            throw new IllegalArgumentException("A card token is required.");
        }

        User currentUser = accessPolicy.requireCurrentUser();
        Account userAccount = accountService.getAccountByUserId(currentUser.getId());

        // 2. Create PENDING Payment Intent in your DB
        Payment payment = Payment.builder()
                .amount(amount)
                .currency(BookingRules.CURRENCY)
                .method(method)
                .status(PaymentStatus.PENDING)
                .transactionId(CodeGenerator.generate(CodePrefixes.PAYMENT))
                .paymentDate(LocalDateTime.now())
                .build();
        payment = paymentRepository.save(payment);

        try {
            // 3. CALL THE REAL STRIPE API
            // Convert Dollars to Cents (e.g., 50.00 * 100 = 5000)
            long amountInCents = amount.movePointRight(2).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();

            Map<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("amount", amountInCents);
            chargeParams.put("currency", BookingRules.CURRENCY.toLowerCase());
            chargeParams.put("source", stripeToken); // The token from the frontend
            chargeParams.put("description", "Wallet Top-Up for " + currentUser.getEmail());

            // This makes the actual HTTP request to Stripe's servers
            Charge charge = Charge.create(chargeParams);

            if (charge.getPaid()) {
                // 4. Success! Mark Completed and Credit the Ledger
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(charge.getId()); // Save Stripe's real receipt ID
                paymentRepository.save(payment);

                return transactionService.creditAccount(
                        userAccount, 
                        amount, 
                        TransactionType.WALLET_TOP_UP, 
                        "Wallet Top-Up via Stripe (Txn: " + charge.getId() + ")", 
                        null
                );
            } else {
                throw new WorkflowException("PAYMENT_NOT_CAPTURED", "The card payment was not captured.");
            }

        } catch (CardException e) {
            // Stripe actively declined the card (e.g., insufficient funds, fraud)
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new WorkflowException("CARD_DECLINED", "Card declined: " + e.getMessage());

        } catch (StripeException e) {
            // General Stripe API errors (e.g., network issue, wrong token format)
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new WorkflowException("PAYMENT_FAILED", "Payment processing error: " + e.getMessage());
        }
    }
}
