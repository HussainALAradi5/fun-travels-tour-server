package com.server.server.controllers.payment;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.payment.TransactionResponse;
import com.server.server.dto.common.PageResponse;
import com.server.server.dto.payment.TransactionFilterRequest;
import com.server.server.enums.payment.TransactionType;
import com.server.server.models.payment.Account;
import com.server.server.models.payment.Transaction;
import com.server.server.services.payment.AccountService;
import com.server.server.services.payment.TransactionService;
import com.server.server.utilities.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final AccountService accountService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER', 'OWNER')")
    public ResponseEntity<ApiResponse<PageResponse<TransactionResponse>>> getTransactions(
            @ModelAttribute TransactionFilterRequest filter) {
        return ResponseEntity.ok(ApiResponse.ok(
                transactionService.filterTransactions(filter).map(TransactionResponse::from)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'MANAGER', 'EMPLOYEE', 'CUSTOMER', 'OWNER')")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(@PathVariable Integer id) {
        return ResponseEntity.ok(ApiResponse.ok(TransactionResponse.from(transactionService.getById(id))));
    }

    @PostMapping("/manual-credit/{userId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<TransactionResponse>> manualCredit(
            @NonNull @PathVariable Integer userId,
            @RequestParam BigDecimal amount,
            @RequestParam String description) {
        Account account = accountService.getAccountByUserId(userId);
        Transaction tx = transactionService.creditAccount(account, amount, TransactionType.MANUAL_ADJUSTMENT, description, null);
        return ResponseEntity.ok(ApiResponse.ok("Credit applied!", TransactionResponse.from(tx)));
    }
}
