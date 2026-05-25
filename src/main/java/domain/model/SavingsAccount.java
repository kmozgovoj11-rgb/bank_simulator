package domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

public class SavingsAccount extends Account {
    private static final Duration INTEREST_ACCRUAL_INTERVAL = Duration.ofDays(30);

    private final BigDecimal interestRate;
    private Instant lastInterestAccrualAt;

    public SavingsAccount(
            String accountId,
            String number,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Customer owner,
            BigDecimal interestRate) {
        this(accountId, number, balance, currency, status, owner, interestRate, null);
    }

    public SavingsAccount(
            String accountId,
            String number,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Customer owner,
            BigDecimal interestRate,
            Instant lastInterestAccrualAt) {
        super(accountId, number, balance, currency, status, owner);
        this.interestRate = requireNonNegative(interestRate, "Interest rate");
        this.lastInterestAccrualAt = lastInterestAccrualAt;
    }

    public BigDecimal calculateMonthlyInterest() {
        return getBalance()
                .multiply(interestRate)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    public boolean canAccrueInterest(Instant now) {
        if (!isActive() || getBalance().signum() <= 0) {
            return false;
        }
        return lastInterestAccrualAt == null
                || !now.isBefore(lastInterestAccrualAt.plus(INTEREST_ACCRUAL_INTERVAL));
    }

    public void markInterestAccrued(Instant accruedAt) {
        if (accruedAt == null) {
            throw new IllegalArgumentException("Interest accrual timestamp is required");
        }
        lastInterestAccrualAt = accruedAt;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public Instant getLastInterestAccrualAt() {
        return lastInterestAccrualAt;
    }
}
