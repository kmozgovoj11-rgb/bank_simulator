package com.banksim.application.service;

import com.banksim.application.validation.AuthCredentialsValidator;
import com.banksim.domain.model.Account;
import com.banksim.domain.model.Customer;
import com.banksim.domain.model.Transaction;
import com.banksim.domain.model.TransferTransaction;
import com.banksim.domain.repository.AccountRepository;
import com.banksim.domain.repository.CustomerRepository;
import com.banksim.domain.repository.TransactionBroker;
import com.banksim.domain.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Прикладной сервис банка: создание клиентов, переводы и чтение истории.
 * Сохранение счетов и транзакций при переводе выполняется внутри одной БД-транзакции через {@link TransactionBroker}.
 */
public class BankService implements BankFacade {
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionBroker transactionBroker;

    public BankService(
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            TransactionRepository transactionRepository,
            TransactionBroker transactionBroker) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.transactionBroker = transactionBroker;
    }

    @Override
    public Customer createCustomer(String customerId, String fullName, String phone) {
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer id is required");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Customer full name is required");
        }
        // Тот же формат, что и логин при входе — единая нормализация телефона.
        String normalizedPhone = AuthCredentialsValidator.normalizeAndValidateLogin(phone);
        Customer customer = new Customer(customerId.trim(), fullName.trim(), normalizedPhone);
        customerRepository.save(customer);
        return customer;
    }

    /**
     * Перевод между счетами: атомарно (в одной транзакции БД) списывает, зачисляет, пишет запись операции и обновляет оба счёта.
     * Массив из одного элемента — обход ограничения лямбды: нужно вернуть созданный {@link TransferTransaction} снаружи {@code inTransaction}.
     */
    @Override
    public TransferTransaction transferMoney(
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            String description) {
        TransferTransaction[] completed = new TransferTransaction[1];
        transactionBroker.inTransaction(() -> {
            Account fromAccount = findRequiredAccount(fromAccountNumber);
            Account toAccount = findRequiredAccount(toAccountNumber);

            TransferTransaction transaction = new TransferTransaction(
                    UUID.randomUUID().toString(),
                    amount,
                    Instant.now(),
                    description,
                    fromAccount,
                    toAccount);
            transaction.execute();

            transactionRepository.save(transaction);
            accountRepository.save(fromAccount);
            accountRepository.save(toAccount);
            completed[0] = transaction;
        });
        return completed[0];
    }

    /** История операций по номеру счёта (включая переводы, где счёт указан как from или to). */
    @Override
    public List<Transaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber);
    }

    @Override
    public Optional<Account> findAccount(String accountNumber) {
        return accountRepository.findByNumber(accountNumber);
    }

    /** Счёт по номеру или исключение, если не найден (для сценариев, где отсутствие счёта — ошибка вызова). */
    private Account findRequiredAccount(String accountNumber) {
        return accountRepository
                .findByNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
    }
}
