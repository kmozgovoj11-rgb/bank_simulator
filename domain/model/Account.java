package domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public abstract class Account {
    private final String accountId;
    private final String number;
    private BigDecimal balance;
    private final String currency;
    private AccountStatus status;
    private final Customer owner;

    protected Account(
            String accountId,
            String number,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Customer owner) {
        this.accountId = requireNonBlank(accountId, "Account id is required");
        this.number = requireNonBlank(number, "Account number is required");
        this.balance = requireNonNegative(balance);
        this.currency = requireNonBlank(currency, "Currency is required");
        this.status = Objects.requireNonNull(status, "Account status is required");
        this.owner = Objects.requireNonNull(owner, "Account owner is required");
    }

    public void deposit(BigDecimal amount) {
        ensureAccountAllowsDepositsAndWithdrawals();
        validatePositiveAmount(amount);
        balance = balance.add(amount);
    }

    public void withdraw(BigDecimal amount) {
        ensureAccountAllowsDepositsAndWithdrawals();
        validatePositiveAmount(amount);
        if (balance.compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance = balance.subtract(amount);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void close() {
        ensureCanBeClosed();
        status = AccountStatus.CLOSED;
    }

    protected void ensureCanBeClosed() {
        if (balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Account can be closed only with zero balance");
        }
    }

    /**
     * Frozen and closed accounts must not accept deposits or withdrawals (including transfer legs).
     */
    protected void ensureAccountAllowsDepositsAndWithdrawals() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Account " + number + " is " + status + "; only ACTIVE accounts allow this operation");
        }
    }

    protected void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static BigDecimal requireNonNegative(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Balance is required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        return value;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getNumber() {
        return number;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public Customer getOwner() {
        return owner;
    }
}
