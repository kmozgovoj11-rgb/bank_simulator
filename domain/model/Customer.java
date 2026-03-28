package domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class Customer {
    private final String customerId;
    private String fullName;
    private String phone;
    private String status;
    private final List<Account> accounts;

    public Customer(String customerId, String fullName, String phone, String status) {
        this.customerId = customerId;
        this.fullName = fullName;
        this.phone = phone;
        this.status = status;
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

    public String getStatus() {
        return status;
    }

    public List<Account> getAccounts() {
        return Collections.unmodifiableList(accounts); //обертка,необходимая для неизменяемости возвращаемого списка
    }

    public void addAccount(Account account) {
        accounts.add(account);
    }
}

