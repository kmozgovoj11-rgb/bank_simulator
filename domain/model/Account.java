package domain.model;

import java.math.BigDecimal;//с суммами лучше через децимал работать

public abstract class Account {
    private final String accountId;
    private final String number;
    private BigDecimal balance;
    private final String currency;
    private AccountStatus status;
    private final Customer owner;

    protected Account(
            String accountId,
            String number,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Customer owner) {
        this.accountId = accountId;
        this.number = number;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.owner = owner;
    }

    public void deposit(BigDecimal amount) {
        validatePositiveAmount(amount);//проверка на положительную сумму
        balance = balance.add(amount);//с децималом нельзя += использовать
    }

    public void withdraw(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (balance.compareTo(amount) < 0) { //сравненивает два числа возвращает -1 меньше суммы в скобках 0 если равно
            throw new IllegalStateException("Insufficient balance");
        }
        balance = balance.subtract(amount);
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void close() {
        status = AccountStatus.CLOSED;
    }

    protected void validatePositiveAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    public String getAccountId() {
        return accountId;
    }

    public String getNumber() {
        return number;
    }

    public String getCurrency() {
        return currency;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public Customer getOwner() {
        return owner;
    }
}

