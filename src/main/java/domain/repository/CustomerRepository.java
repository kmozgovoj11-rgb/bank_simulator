package domain.repository;

import java.util.Optional;

import domain.model.Customer;

public interface CustomerRepository {
    Optional<Customer> findByCustomerId(String customerId);

    Optional<Customer> findByPhone(String phone);

    void save(Customer customer);

    void delete(String customerId);
}
