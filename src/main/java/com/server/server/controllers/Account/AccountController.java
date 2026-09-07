package com.server.server.controllers.Account;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.models.Account;
import com.server.server.models.Transaction;
import com.server.server.services.Account.AccountService;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @GetMapping("/user/{userId}/balance")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN','OWNER','MANAGER')")
    public ResponseEntity<Account> getAccountBalance(@PathVariable Integer userId) {
        Account account = accountService.getAccountByUserId(userId);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/user/{userId}/history")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN')")
    public ResponseEntity<List<Transaction>> getAccountHistory(@PathVariable Integer userId) {
        Account account = accountService.getAccountByUserId(userId);
        List<Transaction> history = accountService.getTransactionHistory(account.getId());
        return ResponseEntity.ok(history);
    }
}