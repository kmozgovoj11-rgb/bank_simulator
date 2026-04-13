package domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Customer {
    private final String customerId;
    private String fullName;
    private String phone;
    private final List<Account> accounts;

    public Customer(String customerId, String fullName, String phone) {
        this.customerId = requireNonBlank(customerId, "Customer id is required");
        this.fullName = requireNonBlank(fullName, "Customer full name is required");
        this.phone = requireNonBlank(phone, "Customer phone is required");
        this.accounts = new ArrayList<>();
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts); // необходимо для неизменяемости возвращаемого списка
    }

    public void addAccount(Account account) {
        accounts.add(account);
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}


