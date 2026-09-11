package com.server.server.services.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.enums.Payment.PaymentStatus;
import com.server.server.enums.TransactionType;
import com.server.server.models.Account;
import com.server.server.models.Payment;
import com.server.server.models.Transaction;
import com.server.server.models.User;
import com.server.server.repositories.PaymentRepository;
import com.server.server.services.TransactionService;
import com.server.server.services.UserService;
import com.stripe.exception.CardException;
import com.stripe.exception.StripeException;
import com.stripe.model.Charge;

@Service
public class WalletTopUpService {

    @Autowired
    private AccountService accountService;
    
    @Autowired
    private TransactionService transactionService;
    
    @Autowired
    private PaymentRepository paymentRepository;
    
    @Autowired
    private UserService userService;

    @Transactional
    public Transaction processWalletTopUp(BigDecimal amount, PaymentMethod method, String stripeToken) {
        
        // 1. Validate Amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        User currentUser = userService.getCurrentUser();
        Account userAccount = accountService.getAccountByUserId(currentUser.getId());

        // 2. Create PENDING Payment Intent in your DB
        Payment payment = Payment.builder()
                .amount(amount)
                .currency("USD")
                .method(method)
                .status(PaymentStatus.PENDING)
                .transactionId("TRX-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paymentDate(LocalDateTime.now())
                .build();
        payment = paymentRepository.save(payment);

        try {
            // 3. CALL THE REAL STRIPE API
            // Convert Dollars to Cents (e.g., 50.00 * 100 = 5000)
            int amountInCents = amount.multiply(new BigDecimal("100")).intValue();

            Map<String, Object> chargeParams = new HashMap<>();
            chargeParams.put("amount", amountInCents);
            chargeParams.put("currency", "usd");
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
                throw new RuntimeException("Payment succeeded but was not marked as paid by Stripe.");
            }

        } catch (CardException e) {
            // Stripe actively declined the card (e.g., insufficient funds, fraud)
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Card Declined: " + e.getMessage());

        } catch (StripeException e) {
            // General Stripe API errors (e.g., network issue, wrong token format)
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Payment processing error: " + e.getMessage());
        }
    }
}
