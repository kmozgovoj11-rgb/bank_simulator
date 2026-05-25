package com.banksim.domain.repository;

import com.banksim.domain.model.Customer;
import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findByCustomerId(String customerId);

    void save(Customer customer);

    void delete(String customerId);
}
