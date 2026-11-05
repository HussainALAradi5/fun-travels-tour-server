package com.server.server.controllers.payment;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.payment.TransactionResponse;
import com.server.server.dto.payment.AccountResponse;
import com.server.server.models.payment.Account;
import com.server.server.services.payment.AccountService;
import com.server.server.utilities.responses.ApiResponse;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/accounts/user/{userId}")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountBalance(@NonNull @PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.ok(AccountResponse.from(accountService.getAccountForViewer(userId))));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAccountHistory(@NonNull @PathVariable Integer userId) {
        Account account = accountService.getAccountForViewer(userId);
        return ResponseEntity.ok(ApiResponse.ok(accountService.getTransactionHistory(account.getId()).stream().map(TransactionResponse::from).toList()));
    }
}
