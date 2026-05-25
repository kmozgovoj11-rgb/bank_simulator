package domain.repository;

import java.util.List;

import domain.model.Transaction;

public interface TransactionRepository {
    void save(Transaction transaction);

    List<Transaction> findByAccountNumber(String accountNumber);
}
