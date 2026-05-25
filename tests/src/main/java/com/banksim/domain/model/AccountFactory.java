package com.banksim.domain.model;

import java.math.BigDecimal;

/**
 * Factory Method pattern: one creation point for concrete account products.
 */
public class AccountFactory {
    public Account createAccount(
            String accountKind,
            String accountId,
            String number,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Customer owner,
            BigDecimal interestRate,
            BigDecimal creditLimit,
            BigDecimal currentDebt) {
        return switch (accountKind) {
            case "DEBIT" -> new DebitAccount(accountId, number, balance, currency, status, owner);
            case "SAVINGS" -> new SavingsAccount(
                    accountId,
                    number,
                    balance,
                    currency,
                    status,
                    owner,
                    interestRate);
            case "CREDIT" -> new CreditAccount(
                    accountId,
                    number,
                    balance,
                    currency,
                    status,
                    owner,
                    creditLimit,
                    currentDebt);
            default -> throw new IllegalArgumentException("Unsupported account kind: " + accountKind);
        };
    }
}
