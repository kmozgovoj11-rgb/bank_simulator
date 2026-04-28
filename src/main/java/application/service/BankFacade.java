package application.service;

import domain.model.Account;
import domain.model.Customer;
import domain.model.Transaction;
import domain.model.TransferTransaction;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;


 //Facade pattern: упрощенный API для банковских сценариев использования.

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