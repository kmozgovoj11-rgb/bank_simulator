package domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/*
  Базовая операция по счетам: шаблон «проверка → выполнение → статус».
  Конкретные типы задают правила валидации и изменение балансов;
 */
public abstract class Transaction implements Command {
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

    // Предусловия операции (счета, сумма, валюта, активность и т.д.) без побочных эффектов. 
    public abstract boolean validate();

    // Применяет команду к домену; при невалидности помечает FAILED и бросает исключение. 
    @Override
    public abstract void execute();

    // Откат команды (обратные движения по счетам); для персистентного слоя может не вызываться.
    @Override
    public abstract void rollback();

    // Номера счетов, по которым эту операцию нужно находить в выборке истории (from/to или одна сторона).
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