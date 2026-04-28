package domain.model;

import java.math.BigDecimal;

public class SavingsAccount extends Account {
    private final BigDecimal interestRate;

    public SavingsAccount(
            String accountId,
            String number,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Customer owner,
            BigDecimal interestRate) {
        super(accountId, number, balance, currency, status, owner);
        this.interestRate = requireNonNegative(interestRate, "Interest rate");
    }

    public BigDecimal calculateInterest() {
        return getBalance().multiply(interestRate);
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }
}
