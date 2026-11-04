package com.server.server.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.server.server.enums.payment.TransactionType;
import com.server.server.models.payment.Transaction;
public record TransactionResponse(
        Integer id,
        BigDecimal amount,
        TransactionType transactionType,
        String description,
        AccountSummary account,
        LocalDateTime timestamp) {

    public static TransactionResponse from(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        return new TransactionResponse(transaction.getId(), transaction.getAmount(), transaction.getType(),
                transaction.getDescription(), AccountSummary.from(transaction.getAccount()), transaction.getTimestamp());
    }
}
