package com.server.server.controllers.Account;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.server.server.config.StripeConfig;
import com.server.server.dto.payment.TransactionResponse;
import com.server.server.enums.Payment.PaymentMethod;
import com.server.server.models.Transaction;
import com.server.server.services.Account.WalletTopUpService;
import com.server.server.utilities.ApiResponse;
import com.server.server.utilities.ModelMapper;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletTopUpService walletTopUpService;
    private final StripeConfig stripeConfig;
    private final ModelMapper modelMapper;

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, String>>> getStripeConfig() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("publishableKey", stripeConfig.getPublishableKey())));
    }

    @PostMapping("/top-up")
    @PreAuthorize("hasAnyAuthority('CUSTOMER', 'ADMIN', 'MANAGER', 'OWNER', 'EMPLOYEE')")
    public ResponseEntity<ApiResponse<TransactionResponse>> topUpWallet(
            @RequestParam BigDecimal amount,
            @RequestParam PaymentMethod method,
            @RequestParam String gatewayToken) {
        Transaction tx = walletTopUpService.processWalletTopUp(amount, method, gatewayToken);
        return ResponseEntity.ok(ApiResponse.ok("Wallet topped up!", modelMapper.toTransactionResponse(tx)));
    }
}
