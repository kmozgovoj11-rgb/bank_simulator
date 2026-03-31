package domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class DepositTransaction extends Transaction {
    private final Account targetAccount;

    public DepositTransaction(
            String transactionId,
            BigDecimal amount,
            Instant timestamp,
            String description,
            Account targetAccount) {
        super(
                transactionId,
                "DEPOSIT",
                amount,
                timestamp,
                TransactionStatus.PENDING,
                description);
        this.targetAccount = targetAccount;
    }

    @Override
    public boolean validate() {
        return targetAccount != null && getAmount() != null && getAmount().signum() > 0;
    }

    @Override
    public void execute() {
        if (!validate()) {
            markFailed();
            throw new IllegalStateException("Deposit validation failed");
        }
        targetAccount.deposit(getAmount());
        markCompleted();
    }

    @Override
    public void rollback() {
        targetAccount.withdraw(getAmount());
        markFailed();
    }

    @Override
    public List<String> getInvolvedAccountNumbers() {
        return List.of(targetAccount.getNumber());
    }

    public Account getTargetAccount() {
        return targetAccount;
    }
}

