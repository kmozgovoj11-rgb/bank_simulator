package com.banksim.application.service;

import com.banksim.domain.model.Account;
import com.banksim.domain.model.Customer;
import com.banksim.domain.model.Transaction;
import com.banksim.domain.model.TransferTransaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Facade pattern: simplified API for banking use cases.
 */
public interface BankFacade {
    Customer createCustomer(String customerId, String fullName, String phone);

    TransferTransaction transferMoney(
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            String description);

    List<Transaction> getAccountHistory(String accountNumber);

    Optional<Account> findAccount(String accountNumber);
}
