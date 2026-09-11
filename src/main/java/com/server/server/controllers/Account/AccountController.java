package com.server.server.controllers.Account;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.dto.payment.TransactionResponse;
import com.server.server.dto.support.AccountResponse;
import com.server.server.models.Account;
import com.server.server.services.Account.AccountService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts/user/{userId}")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;
    private final ModelMapper modelMapper;

    @GetMapping("/balance")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccountBalance(@NonNull @PathVariable Integer userId) {
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toAccountResponse(accountService.getAccountByUserId(userId))));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getAccountHistory(@NonNull @PathVariable Integer userId) {
        Account account = accountService.getAccountByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(modelMapper.toTransactionResponseList(accountService.getTransactionHistory(account.getId()))));
    }
}
