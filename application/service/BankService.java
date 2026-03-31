package application.service;

import domain.model.Account;
import domain.model.Customer;
import domain.model.Transaction;
import domain.model.TransferTransaction;
import domain.repository.AccountRepository;
import domain.repository.CustomerRepository;
import domain.repository.TransactionRepository;
import domain.repository.TransactionBroker;
import java.math.BigDecimal;
import java.time.Instant;//для текущего времени
import java.util.List;//
import java.util.Optional;
import java.util.UUID;//для уникального идентификатора

public class BankService {
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

    public Customer createCustomer(String customerId, String fullName, String phone) {
        Customer customer = new Customer(customerId, fullName, phone);
        customerRepository.save(customer);
        return customer;
    }

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
        transaction.execute();//выполнение транзакции

        transactionRepository.save(transaction);
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        completed[0] = transaction;
            });
        return completed[0];
    }

    public List<Transaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber);
    }

    public Optional<Account> findAccount(String accountNumber) {
        return accountRepository.findByNumber(accountNumber);
    }

    private Account findRequiredAccount(String accountNumber) {
        return accountRepository
                .findByNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
    }
}
