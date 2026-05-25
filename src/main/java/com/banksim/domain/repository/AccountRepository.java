package com.banksim.domain.repository;

import com.banksim.domain.model.Account;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findByNumber(String number);

    void save(Account account);

    List<Account> findByCustomer(String customerId);

    void delete(String accountId);
}
