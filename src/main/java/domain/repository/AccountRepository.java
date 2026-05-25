package domain.repository;

import java.util.List;
import java.util.Optional;

import domain.model.Account;

public interface AccountRepository {
    Optional<Account> findByNumber(String number);

    void save(Account account);

    List<Account> findByCustomer(String customerId);

    void delete(String accountId);
}