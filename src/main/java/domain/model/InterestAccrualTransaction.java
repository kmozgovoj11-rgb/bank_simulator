package domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Monthly interest accrual for a savings account. */
public class InterestAccrualTransaction extends Transaction {
    private final SavingsAccount targetAccount;

    public InterestAccrualTransaction(
            String transactionId,
            BigDecimal amount,
            Instant timestamp,
            String description,
            SavingsAccount targetAccount) {
        super(
                transactionId,
                "INTEREST",
                amount,
                timestamp,
                TransactionStatus.PENDING,
                description);
        this.targetAccount = targetAccount;
    }

    @Override
    public boolean validate() {
        return targetAccount != null
                && targetAccount.isActive()
                && getAmount() != null
                && getAmount().signum() > 0;
    }

    @Override
    public void execute() {
        if (!validate()) {
            markFailed();
            throw new IllegalStateException("Interest accrual validation failed");
        }
        targetAccount.deposit(getAmount());
        markCompleted();
    }

    @Override
    public void rollback() {
        targetAccount.rollbackDeposit(getAmount());
        markFailed();
    }

    @Override
    public List<String> getInvolvedAccountNumbers() {
        return List.of(targetAccount.getNumber());
    }

    public SavingsAccount getTargetAccount() {
        return targetAccount;
    }
}
