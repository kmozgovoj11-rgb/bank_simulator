package application.service;

import domain.model.Account;
import domain.model.Customer;
import domain.model.DepositTransaction;
import domain.model.InterestAccrualTransaction;
import domain.model.Transaction;
import domain.model.TransferTransaction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Паттерн Facade: простой набор операций для работы с банковской подсистемой.
 * Скрывает детали репозиториев, БД-транзакций и доменных команд (Transaction).
 * Внешние слои (HTTP-сервис, CustomerPortalService) зависят только от этого интерфейса,
 * а не от реализации {@link BankService}.
 */
public interface BankFacade {
    /** Создать нового клиента с нормализованным телефоном. */
    Customer createCustomer(String customerId, String fullName, String phone);

    /** Перевод между счетами; всё списание/зачисление/запись истории атомарно в одной БД-транзакции. */
    TransferTransaction transferMoney(
            String fromAccountNumber,
            String toAccountNumber,
            BigDecimal amount,
            String description);

    /** Пополнение счёта (внешнее зачисление). */
    DepositTransaction depositMoney(String accountNumber, BigDecimal amount, String description);

    /** Начисление процентов на сберегательный счёт (раз в месяц). */
    InterestAccrualTransaction accrueInterest(String accountNumber);

    /** История операций по номеру счёта (включая переводы как от/к). */
    List<Transaction> getAccountHistory(String accountNumber);

    /** Поиск счёта по номеру. */
    Optional<Account> findAccount(String accountNumber);
}
