package domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Базовый класс банковской операции.
 * Реализует паттерн Command: каждая операция знает, как себя выполнить ({@link #execute()})
 * и как откатить ({@link #rollback()}). Конкретные подклассы — DepositTransaction,
 * WithdrawTransaction, TransferTransaction, InterestAccrualTransaction.
 */
public abstract class Transaction implements Command {
    private static final int MONEY_SCALE = 2;
    private static final RoundingMode MONEY_ROUNDING = RoundingMode.HALF_UP;

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
        this.amount = normalizeAmount(amount);
        this.timestamp = timestamp;
        this.status = status;
        this.description = description;
    }

    public abstract boolean validate();

    @Override
    public abstract void execute();

    @Override
    public abstract void rollback();

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

    private static BigDecimal normalizeAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Transaction amount is required");
        }
        return amount.setScale(MONEY_SCALE, MONEY_ROUNDING);
    }
}
