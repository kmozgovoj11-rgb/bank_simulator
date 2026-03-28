package domain.model;

import java.math.BigDecimal;

public class CreditAccount extends Account {
    private final BigDecimal creditLimit;
    private BigDecimal currentDebt;

    public CreditAccount(
            String accountId,
            String number,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Customer owner,
            BigDecimal creditLimit,
            BigDecimal currentDebt) {
        super(accountId, number, balance, currency, status, owner);
        this.creditLimit = creditLimit;
        this.currentDebt = currentDebt;
    }

    @Override
    public void withdraw(BigDecimal amount) {
        validatePositiveAmount(amount);
        BigDecimal totalAvailable = getBalance().add(creditLimit).subtract(currentDebt);
        if (totalAvailable.compareTo(amount) < 0) {
            throw new IllegalStateException("Credit limit exceeded");
        }
        if (getBalance().compareTo(amount) >= 0) {
            super.withdraw(amount);
        } else {
            BigDecimal remaining = amount.subtract(getBalance());
            super.withdraw(getBalance());
            currentDebt = currentDebt.add(remaining);
        }
    }

    public void repayDebt(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (currentDebt.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (currentDebt.compareTo(amount) <= 0) {
            BigDecimal excess = amount.subtract(currentDebt);
            currentDebt = BigDecimal.ZERO;
            if (excess.compareTo(BigDecimal.ZERO) > 0) {
                deposit(excess);
            }
            return;
        }
        currentDebt = currentDebt.subtract(amount);
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getCurrentDebt() {
        return currentDebt;
    }
}