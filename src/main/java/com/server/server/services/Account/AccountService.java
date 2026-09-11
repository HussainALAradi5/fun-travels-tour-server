package com.server.server.services.Account;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.Account.AccountStatus;
import com.server.server.enums.Account.AccountType;
import com.server.server.models.Account;
import com.server.server.models.Transaction;
import com.server.server.models.User;
import com.server.server.repositories.AccountRepository;
import com.server.server.repositories.TransactionRepository;
import com.server.server.repositories.UserRepository; // <-- ADD THIS IMPORT

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    private final TransactionRepository transactionRepository;

    // 1. ADD USER REPOSITORY TO FETCH THE USER IF ACCOUNT IS MISSING
    private final UserRepository userRepository;

    @Transactional
    public Account createAccountForUser(User user) {
        if (accountRepository.findByUserId(user.getId()).isPresent()) {
            throw new RuntimeException("User already has an active account.");
        }

        Account account = new Account();
        account.setUser(user);
        account.setBalance(BigDecimal.ZERO);
        account.setAccountNumber("ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        account.setAccountName(user.getName() + "'s Digital Wallet");
        
        // Explicitly set the types
        account.setType(AccountType.CUSTOMER_WALLET);
        account.setStatus(AccountStatus.ACTIVE);

        return accountRepository.save(account);
    }

    // 2. THE AUTO-HEAL FIX
    @Transactional
    public Account getAccountByUserId(@NonNull Integer userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // If account is missing, find the user and generate an account instantly!
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new RuntimeException("User not found"));
                    return createAccountForUser(user);
                });
    }

    public List<Transaction> getTransactionHistory(@NonNull Integer accountId) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        return transactionRepository.findByAccountIdOrderByTimestampDesc(accountId);
    }
}
