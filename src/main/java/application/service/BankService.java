package application.service;

import application.validation.AuthCredentialsValidator;
import domain.model.Account;
import domain.model.Customer;
import domain.model.DepositTransaction;
import domain.model.InterestAccrualTransaction;
import domain.model.SavingsAccount;
import domain.model.Transaction;
import domain.model.TransferTransaction;
import domain.repository.AccountRepository;
import domain.repository.CustomerRepository;
import domain.repository.TransactionBroker;
import domain.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Реализация фасада банковской подсистемы ({@link BankFacade}).
 * Координирует репозитории и {@link TransactionBroker}, скрывая детали от внешнего кода.
 * Сохранение счетов и транзакций выполняется внутри одной БД-транзакции через {@link TransactionBroker}.
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

    @Override
    public DepositTransaction depositMoney(String accountNumber, BigDecimal amount, String description) {
        DepositTransaction[] completed = new DepositTransaction[1];
        transactionBroker.inTransaction(() -> {
            Account targetAccount = findRequiredAccount(accountNumber);

            DepositTransaction transaction = new DepositTransaction(
                    UUID.randomUUID().toString(),
                    amount,
                    Instant.now(),
                    description,
                    targetAccount);
            transaction.execute();

            transactionRepository.save(transaction);
            accountRepository.save(targetAccount);
            completed[0] = transaction;
        });
        return completed[0];
    }

    @Override
    public InterestAccrualTransaction accrueInterest(String accountNumber) {
        InterestAccrualTransaction[] completed = new InterestAccrualTransaction[1];
        transactionBroker.inTransaction(() -> {
            Account account = findRequiredAccount(accountNumber);
            if (!(account instanceof SavingsAccount savingsAccount)) {
                throw new IllegalArgumentException("Interest can be accrued only for savings accounts");
            }

            Instant now = Instant.now();
            if (!savingsAccount.canAccrueInterest(now)) {
                throw new IllegalStateException("Interest has already been accrued recently or account is not eligible");
            }

            BigDecimal interest = savingsAccount.calculateMonthlyInterest();
            if (interest.signum() <= 0) {
                throw new IllegalStateException("Calculated interest amount must be positive");
            }

            InterestAccrualTransaction transaction = new InterestAccrualTransaction(
                    UUID.randomUUID().toString(),
                    interest,
                    now,
                    "Начисление процентов за месяц",
                    savingsAccount);
            transaction.execute();
            savingsAccount.markInterestAccrued(now);

            transactionRepository.save(transaction);
            accountRepository.save(savingsAccount);
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
