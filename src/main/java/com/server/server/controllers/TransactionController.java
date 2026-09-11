package com.server.server.controllers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.payment.TransactionResponse;
import com.server.server.enums.TransactionType;
import com.server.server.models.Account;
import com.server.server.models.Transaction;
import com.server.server.services.Account.AccountService;
import com.server.server.services.TransactionService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final ModelMapper modelMapper;

    @GetMapping("/filter")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER', 'OWNER')")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> filterTransactions(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Long agencyId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false, defaultValue = "timestamp") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toTransactionResponseList(transactionService.filterTransactions(
                userId, type, startDate, endDate, agencyId, branchId, sortBy, sortDir))));
    }

    @PostMapping("/manual-credit/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> manualCredit(
            @NonNull @PathVariable Integer userId,
            @RequestParam BigDecimal amount,
            @RequestParam String description) {
        Account account = accountService.getAccountByUserId(userId);
        Transaction tx = transactionService.creditAccount(account, amount, TransactionType.MANUAL_ADJUSTMENT, description, null);
        return ResponseEntity.ok(ApiResponse.ok("Credit applied!", modelMapper.toTransactionResponse(tx)));
    }
}
