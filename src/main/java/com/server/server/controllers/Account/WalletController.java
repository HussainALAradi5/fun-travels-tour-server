package com.server.server.controllers.Account;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.config.StripeConfig;
import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.models.Transaction;
import com.server.server.services.Account.WalletTopUpService;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    @Autowired
    private WalletTopUpService walletTopUpService;

    @Autowired
    private StripeConfig stripeConfig;

    // FIX: Changed to permitAll(). The publishable key is completely public!
    @GetMapping("/config")
    @PreAuthorize("permitAll()") 
    public ResponseEntity<Map<String, String>> getStripeConfig() {
        return ResponseEntity.ok(Map.of("publishableKey", stripeConfig.getPublishableKey()));
    }

    @PostMapping("/top-up")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'OWNER', 'EMPLOYEE')")
    public ResponseEntity<?> topUpWallet(
            @RequestParam BigDecimal amount,
            @RequestParam PaymentMethod method,
            @RequestParam(required = true) String gatewayToken) {
        
        try {
            Transaction tx = walletTopUpService.processWalletTopUp(amount, method, gatewayToken);
            return ResponseEntity.ok(tx);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }
}