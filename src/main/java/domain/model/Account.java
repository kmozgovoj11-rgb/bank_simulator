package domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public abstract class Account {
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

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
        this.balance = requireNonNegative(balance, "Balance");
        this.currency = requireNonBlank(currency, "Currency is required");
        this.status = Objects.requireNonNull(status, "Account status is required");
        this.owner = Objects.requireNonNull(owner, "Account owner is required");
    }

    public void deposit(BigDecimal amount) {
        ensureAccountAllowsDepositsAndWithdrawals();
        BigDecimal normalizedAmount = normalizePositiveAmount(amount);
        balance = balance.add(normalizedAmount);
    }

    public void withdraw(BigDecimal amount) {
        ensureAccountAllowsDepositsAndWithdrawals();
        BigDecimal normalizedAmount = normalizePositiveAmount(amount);
        if (balance.compareTo(normalizedAmount) < 0) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance = balance.subtract(normalizedAmount);
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

    protected void ensureAccountAllowsDepositsAndWithdrawals() {
        if (status != AccountStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Account " + number + " is " + status + "; only ACTIVE accounts allow this operation");
        }
    }

    protected void validatePositiveAmount(BigDecimal amount) {
        normalizePositiveAmount(amount);
    }

    protected void rollbackWithdraw(BigDecimal amount) {
        BigDecimal normalizedAmount = normalizePositiveAmount(amount);
        balance = balance.add(normalizedAmount);
    }

    protected void rollbackDeposit(BigDecimal amount) {
        BigDecimal normalizedAmount = normalizePositiveAmount(amount);
        if (balance.compareTo(normalizedAmount) < 0) {
            throw new IllegalStateException("Cannot rollback deposit: balance is smaller than the rollback amount");
        }
        balance = balance.subtract(normalizedAmount);
    }

    protected static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    protected static BigDecimal requireNonNegative(BigDecimal value, String fieldName) {
        BigDecimal normalizedValue = normalizeMoney(value, fieldName);
        if (normalizedValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
        return normalizedValue;
    }

    protected static BigDecimal normalizeMoney(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }

    protected static BigDecimal normalizePositiveAmount(BigDecimal amount) {
        BigDecimal normalizedAmount = normalizeMoney(amount, "Amount");
        if (normalizedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        return normalizedAmount;
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
