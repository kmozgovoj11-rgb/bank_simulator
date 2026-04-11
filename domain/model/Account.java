package domain.model;

import java.math.BigDecimal;//с суммами лучше через децимал работать


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
        this.accountId = accountId;
        this.number = number;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.owner = owner;
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
        status = AccountStatus.CLOSED;
    }

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
