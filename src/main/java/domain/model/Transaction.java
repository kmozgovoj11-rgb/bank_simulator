package domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public abstract class Transaction {
    private final String transactionId;
    private final String type;
    private final BigDecimal amount;
    private final Instant timestamp;
    private TransactionStatus status;
    private final String description;

    protected Transaction(
            String transactionId,
            String type,
            BigDecimal amount,
            Instant timestamp,
            TransactionStatus status,
            String description) {
        this.transactionId = transactionId;
        this.type = type;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
        this.description = description;
    }

    public abstract boolean validate();

    public abstract void execute();

    public abstract void rollback();

    // Номера счетов, на которые влияет данная транзакция (для запросов истории)
    public abstract List<String> getInvolvedAccountNumbers();

    protected void markCompleted() {
        status = TransactionStatus.COMPLETED;
    }

    protected void markFailed() {
        status = TransactionStatus.FAILED;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }
}
