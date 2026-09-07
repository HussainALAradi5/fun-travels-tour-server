package com.server.server.controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.enums.TransactionType;
import com.server.server.models.Account;
import com.server.server.models.Transaction;
import com.server.server.services.Account.AccountService;
import com.server.server.services.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    @GetMapping("/filter")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER', 'OWNER')")
    public ResponseEntity<List<Transaction>> filterTransactions(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long agencyId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false, defaultValue = "timestamp") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {

        return ResponseEntity.ok(transactionService.filterTransactions(
                userId, type, startDate, endDate, agencyId, branchId, sortBy, sortDir));
    }

@PostMapping("/manual-credit/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<?> manualCredit( // Changed to <?> to return String error messages
            @PathVariable Integer userId,
            @RequestParam BigDecimal amount,
            @RequestParam String description) {

        // SECURITY FIX: Prevent Admins from crediting negative or zero amounts
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Manual credit amount must be greater than zero.");
        }

        try {
            Account account = accountService.getAccountByUserId(userId);
            Transaction tx = transactionService.creditAccount(
                    account,
                    amount,
                    TransactionType.MANUAL_ADJUSTMENT,
                    description,
                    null 
            );
            return ResponseEntity.ok(tx);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
