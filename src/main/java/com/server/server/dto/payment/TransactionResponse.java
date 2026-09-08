package com.server.server.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.server.server.enums.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionResponse {
    private Integer id;
    private BigDecimal amount;
    private TransactionType transactionType;
    private String description;
    private AccountSummary account;
    private LocalDateTime timestamp;

    @Data @AllArgsConstructor @NoArgsConstructor
    public static class AccountSummary {
        private Integer id;
        private String accountNumber;
        private String accountName;
    }
}
