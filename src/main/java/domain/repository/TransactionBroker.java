package domain.repository;

public interface TransactionBroker {
    void inTransaction(Runnable action);
}
