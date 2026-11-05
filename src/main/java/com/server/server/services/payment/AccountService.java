package com.server.server.services.payment;

import com.server.server.constants.common.CodePrefixes;

import com.server.server.utilities.codes.CodeGenerator;

import com.server.server.exceptions.common.ResourceNotFoundException;

import com.server.server.exceptions.common.DuplicateResourceException;

import com.server.server.services.auth.AccessPolicyService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.server.server.enums.payment.AccountStatus;
import com.server.server.enums.payment.AccountType;
import com.server.server.models.payment.Account;
import com.server.server.models.payment.Transaction;
import com.server.server.models.user.User;
import com.server.server.repositories.payment.AccountRepository;
import com.server.server.repositories.payment.TransactionRepository;
import com.server.server.repositories.user.UserRepository; // <-- ADD THIS IMPORT

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    private final TransactionRepository transactionRepository;

    private final UserRepository userRepository;
    private final AccessPolicyService accessPolicy;

    @Transactional
    public Account createAccountForUser(User user) {
        if (accountRepository.findByUserId(user.getId()).isPresent()) {
            throw new DuplicateResourceException("User already has an active account.");
        }

        Account account = new Account();
        account.setUser(user);
        account.setBalance(BigDecimal.ZERO);
        account.setAccountNumber(CodeGenerator.generate(CodePrefixes.ACCOUNT));
        account.setAccountName(user.getName() + "'s Digital Wallet");
        
        // Explicitly set the types
        account.setType(AccountType.CUSTOMER_WALLET);
        account.setStatus(AccountStatus.ACTIVE);

        return accountRepository.save(account);
    }

    /** The user's wallet, created on first use for accounts that predate wallets. */
    @Transactional
    public Account getAccountByUserId(@NonNull Integer userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // If account is missing, find the user and generate an account instantly!
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User", userId));
                    return createAccountForUser(user);
                });
    }

    /** Wallet of a user, visible to that user and to administrators. */
    @Transactional
    public Account getAccountForViewer(@NonNull Integer userId) {
        accessPolicy.requireSelfOrAdmin(userId);
        return getAccountByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactionHistory(@NonNull Integer accountId) {
        Objects.requireNonNull(accountId, "accountId must not be null");
        return transactionRepository.findByAccountIdOrderByTimestampDesc(accountId);
    }
}
