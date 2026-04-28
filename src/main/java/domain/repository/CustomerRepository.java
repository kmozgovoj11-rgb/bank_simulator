package domain.repository;

import domain.model.Customer;
import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findByCustomerId(String customerId);

    void save(Customer customer);

    void delete(String customerId);
}