package domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class TransferTransaction extends Transaction {
    private final Account fromAccount;
    private final Account toAccount;

    public TransferTransaction(
            String transactionId,
            BigDecimal amount,
            Instant timestamp,
            String description,
            Account fromAccount,
            Account toAccount) {
        super(
                transactionId,
                "TRANSFER",
                amount,
                timestamp,
                TransactionStatus.PENDING,
                description);
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
    }

    @Override
    public boolean validate() {
        if (fromAccount == null || toAccount == null || getAmount() == null || getAmount().signum() <= 0) {
            return false;
        }
        if (fromAccount == toAccount || fromAccount.getNumber().equals(toAccount.getNumber())) {
            return false;
        }
        if (!fromAccount.isActive() || !toAccount.isActive()) {
            return false;
        }
        return fromAccount.getCurrency().equals(toAccount.getCurrency());
    }

    @Override
    public void execute() {
        if (!validate()) {
            markFailed();
            throw new IllegalStateException("Transfer validation failed");
        }
        fromAccount.withdraw(getAmount());
        toAccount.deposit(getAmount());
        markCompleted();
    }

    @Override
    public void rollback() {
        toAccount.withdraw(getAmount());
        fromAccount.deposit(getAmount());
        markFailed();
    }

    public Account getFromAccount() {
        return fromAccount;
    }

    public Account getToAccount() {
        return toAccount;
    }

    @Override
    public List<String> getInvolvedAccountNumbers() {
        return List.of(fromAccount.getNumber(), toAccount.getNumber());
    }
}
