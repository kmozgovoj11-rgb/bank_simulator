package com.banksim.domain.repository;

public interface TransactionBroker {
    void inTransaction(Runnable action);
}
