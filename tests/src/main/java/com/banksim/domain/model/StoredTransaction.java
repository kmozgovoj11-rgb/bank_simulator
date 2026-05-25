package com.banksim.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Read-only transaction loaded from persistence (or re-saved journal row). Cannot be executed again.
 */
public final class StoredTransaction extends Transaction {
    private final String currency;
    private final String fromAccountNumber;
    private final String toAccountNumber;

    public StoredTransaction(
            String transactionId,
            String type,
            BigDecimal amount,
            Instant timestamp,
            TransactionStatus status,
            String description,
            String currency,
            String fromAccountNumber,
            String toAccountNumber) {
        super(transactionId, type, amount, timestamp, status, description);
        this.currency = currency;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        validateNumbers(type, fromAccountNumber, toAccountNumber);
    }

    private static void validateNumbers(String type, String from, String to) {
        switch (type) {
            case "DEPOSIT" -> {
                if (to == null) {
                    throw new IllegalArgumentException("DEPOSIT requires toAccountNumber");
                }
            }
            case "WITHDRAW" -> {
                if (from == null) {
                    throw new IllegalArgumentException("WITHDRAW requires fromAccountNumber");
                }
            }
            case "TRANSFER" -> {
                if (from == null || to == null) {
                    throw new IllegalArgumentException("TRANSFER requires both account numbers");
                }
            }
            default -> {
                // allow future types without strict check
            }
        }
    }

    public String getCurrency() {
        return currency;
    }

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    @Override
    public boolean validate() {
        return true;
    }

    @Override
    public void execute() {
        throw new UnsupportedOperationException("Persisted transaction record is read-only");
    }

    @Override
    public void rollback() {
        throw new UnsupportedOperationException("Persisted transaction record is read-only");
    }

    @Override
    public java.util.List<String> getInvolvedAccountNumbers() {
        return switch (getType()) {
            case "DEPOSIT" -> java.util.List.of(toAccountNumber);
            case "WITHDRAW" -> java.util.List.of(fromAccountNumber);
            case "TRANSFER" -> java.util.List.of(fromAccountNumber, toAccountNumber);
            default -> throw new IllegalStateException("Unsupported history type: " + getType());
        };
    }
}
