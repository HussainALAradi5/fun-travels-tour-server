package com.server.server.dto.payment;

import java.math.BigDecimal;

import com.server.server.models.payment.Account;
public record AccountResponse(
        Integer id,
        String accountNumber,
        String accountName,
        BigDecimal balance,
        String type,
        String status) {

    public static AccountResponse from(Account account) {
        if (account == null) {
            return null;
        }
        return new AccountResponse(account.getId(), account.getAccountNumber(), account.getAccountName(),
                account.getBalance(), account.getType() == null ? null : account.getType().name(),
                account.getStatus() == null ? null : account.getStatus().name());
    }
}
