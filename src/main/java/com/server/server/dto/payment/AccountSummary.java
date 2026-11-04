package com.server.server.dto.payment;

import com.server.server.models.payment.Account;
/** A wallet account by id, number, and name. */
public record AccountSummary(Integer id, String accountNumber, String accountName) {

    public static AccountSummary from(Account source) {
        return source == null ? null : new AccountSummary(source.getId(), source.getAccountNumber(), source.getAccountName());
    }
}
