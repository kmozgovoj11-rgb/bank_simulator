package domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Снятие с одного счёта; лимиты кредита и достаточность средств обрабатываются в {@link Account#withdraw} (в т.ч. {@link CreditAccount}). */
public class WithdrawTransaction extends Transaction {
    private final Account sourceAccount;

    public WithdrawTransaction(
            String transactionId,
            BigDecimal amount,
            Instant timestamp,
            String description,
            Account sourceAccount) {
        super(
                transactionId,
                "WITHDRAW",
                amount,
                timestamp,
                TransactionStatus.PENDING,
                description);
        this.sourceAccount = sourceAccount;
    }

    @Override
    public boolean validate() {
        return sourceAccount != null
                && sourceAccount.isActive()
                && getAmount() != null
                && getAmount().signum() > 0;
    }

    @Override
    public void execute() {
        if (!validate()) {
            markFailed();
            throw new IllegalStateException("Withdraw validation failed");
        }
        sourceAccount.withdraw(getAmount());
        markCompleted();
    }

    @Override
    public void rollback() {
        sourceAccount.rollbackWithdraw(getAmount());
        markFailed();
    }

    @Override
    public List<String> getInvolvedAccountNumbers() {
        return List.of(sourceAccount.getNumber());
    }

    public Account getSourceAccount() {
        return sourceAccount;
    }
}

