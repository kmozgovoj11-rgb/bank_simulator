package domain.model;

import java.math.BigDecimal;
import java.time.Instant;
//Транзакция только для чтения из базы данных
//Используется для отображения истории операций
//Нельзя выполнить или откатить
public final class StoredTransaction extends Transaction {
    private final String currency;
    private final String fromAccountNumber;
    private final String toAccountNumber;

    public StoredTransaction(
            String transactionId,
            String type,
            BigDecimal amount,
            Instant timestamp,
            TransactionStatus status,
            String description,
            String currency,
            String fromAccountNumber,
            String toAccountNumber) {
        super(transactionId, type, amount, timestamp, status, description);
        this.currency = currency;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        validateNumbers(type, fromAccountNumber, toAccountNumber);
    }

    private static void validateNumbers(String type, String from, String to) {
        switch (type) {
            case "DEPOSIT" -> {
                if (to == null) {
                    throw new IllegalArgumentException("DEPOSIT requires toAccountNumber");
                }
            }
            case "WITHDRAW" -> {
                if (from == null) {
                    throw new IllegalArgumentException("WITHDRAW requires fromAccountNumber");
                }
            }
            case "TRANSFER" -> {
                if (from == null || to == null) {
                    throw new IllegalArgumentException("TRANSFER requires both account numbers");
                }
            }
            default -> {
            }
        }
    }

    public String getCurrency() {
        return currency;
    }

    public String getFromAccountNumber() {
        return fromAccountNumber;
    }

    public String getToAccountNumber() {
        return toAccountNumber;
    }

    @Override
    public boolean validate() {  //транзация валидна
        return true;
    }

    @Override
    public void execute() {
        //Защита от случайного выполнения старых транзакций из истории
        throw new UnsupportedOperationException("Persisted transaction record is read-only");
    }

    @Override
    public void rollback() {
        //Защита от случайного выполнения старых транзакций из истории
        throw new UnsupportedOperationException("Persisted transaction record is read-only");
    }

    @Override
    public java.util.List<String> getInvolvedAccountNumbers() {
        return switch (getType()) {
            case "DEPOSIT" -> java.util.List.of(toAccountNumber);
            case "WITHDRAW" -> java.util.List.of(fromAccountNumber);
            case "TRANSFER" -> java.util.List.of(fromAccountNumber, toAccountNumber);
            default -> throw new IllegalStateException("Unsupported history type: " + getType());
        };
    }
}
