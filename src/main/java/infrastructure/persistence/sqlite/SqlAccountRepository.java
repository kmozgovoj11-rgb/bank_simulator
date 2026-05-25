package infrastructure.persistence.sqlite;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import domain.model.Account;
import domain.model.AccountFactory;
import domain.model.AccountStatus;
import domain.model.CreditAccount;
import domain.model.Customer;
import domain.model.DebitAccount;
import domain.model.SavingsAccount;
import domain.repository.AccountRepository;

public class SqlAccountRepository implements AccountRepository {
    private static final String SELECT_BASE =
            """
            SELECT a.account_id, a.number, a.customer_id, a.account_kind, a.balance, a.currency, a.status,
                   a.interest_rate, a.last_interest_accrual_ms, a.credit_limit, a.current_debt,
                   c.full_name AS customer_full_name, c.phone AS customer_phone
            FROM accounts a
            JOIN customers c ON c.customer_id = a.customer_id
            """;

    private final Database database;

    public SqlAccountRepository(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public Optional<Account> findByNumber(String number) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                return findByNumber(ctx, number);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
        try (Connection connection = database.openConnection()) {
            return findByNumber(connection, number);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Optional<Account> findByNumber(Connection connection, String number) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BASE + " WHERE a.number = ?")) {
            ps.setString(1, number);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                Customer owner = mapOwner(rs);
                List<Account> ownerAccounts = loadAccountsForCustomer(connection, owner);
                return ownerAccounts.stream()
                        .filter(account -> account.getNumber().equals(number))
                        .findFirst();
            }
        }
    }

    @Override
    public void save(Account account) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                save(ctx, account);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return;
        }
        try (Connection connection = database.openConnection()) {
            save(connection, account);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void save(Connection connection, Account account) throws SQLException {
        String kind = accountKind(account);
        String interestRate = null;
        Long lastInterestAccrualMs = null;
        String creditLimit = null;
        String currentDebt = null;
        if (account instanceof SavingsAccount savingsAccount) {
            interestRate = savingsAccount.getInterestRate().toPlainString();
            if (savingsAccount.getLastInterestAccrualAt() != null) {
                lastInterestAccrualMs = savingsAccount.getLastInterestAccrualAt().toEpochMilli();
            }
        } else if (account instanceof CreditAccount creditAccount) {
            creditLimit = creditAccount.getCreditLimit().toPlainString();
            currentDebt = creditAccount.getCurrentDebt().toPlainString();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO accounts (
                    account_id, number, customer_id, account_kind, balance, currency, status,
                    interest_rate, last_interest_accrual_ms, credit_limit, current_debt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(account_id) DO UPDATE SET
                    number = excluded.number,
                    customer_id = excluded.customer_id,
                    account_kind = excluded.account_kind,
                    balance = excluded.balance,
                    currency = excluded.currency,
                    status = excluded.status,
                    interest_rate = excluded.interest_rate,
                    last_interest_accrual_ms = excluded.last_interest_accrual_ms,
                    credit_limit = excluded.credit_limit,
                    current_debt = excluded.current_debt
                """)) {
            ps.setString(1, account.getAccountId());
            ps.setString(2, account.getNumber());
            ps.setString(3, account.getOwner().getCustomerId());
            ps.setString(4, kind);
            ps.setString(5, account.getBalance().toPlainString());
            ps.setString(6, account.getCurrency());
            ps.setString(7, account.getStatus().name());
            ps.setString(8, interestRate);
            if (lastInterestAccrualMs == null) {
                ps.setNull(9, Types.BIGINT);
            } else {
                ps.setLong(9, lastInterestAccrualMs);
            }
            ps.setString(10, creditLimit);
            ps.setString(11, currentDebt);
            ps.executeUpdate();
        }
    }

    private static String accountKind(Account account) {
        if (account instanceof DebitAccount) {
            return "DEBIT";
        }
        if (account instanceof SavingsAccount) {
            return "SAVINGS";
        }
        if (account instanceof CreditAccount) {
            return "CREDIT";
        }
        throw new IllegalArgumentException("Unsupported account implementation: " + account.getClass().getName());
    }

    @Override
    public List<Account> findByCustomer(String customerId) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                return findByCustomer(ctx, customerId);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
        try (Connection connection = database.openConnection()) {
            return findByCustomer(connection, customerId);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Account> findByCustomer(Connection connection, String customerId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BASE + " WHERE a.customer_id = ? ORDER BY a.number")) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return List.of();
                }
                Customer owner = mapOwner(rs);
                return loadAccountsForCustomer(connection, owner);
            }
        }
    }

    private static List<Account> loadAccountsForCustomer(Connection connection, Customer owner) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(SELECT_BASE + " WHERE a.customer_id = ? ORDER BY a.number")) {
            ps.setString(1, owner.getCustomerId());
            try (ResultSet rs = ps.executeQuery()) {
                List<Account> accounts = new ArrayList<>();
                while (rs.next()) {
                    Account account = mapAccount(rs, owner);
                    owner.addAccount(account);
                    accounts.add(account);
                }
                return List.copyOf(accounts);
            }
        }
    }

    @Override
    public void delete(String accountId) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                delete(ctx, accountId);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return;
        }
        try (Connection connection = database.openConnection()) {
            delete(connection, accountId);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void delete(Connection connection, String accountId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM accounts WHERE account_id = ?")) {
            ps.setString(1, accountId);
            ps.executeUpdate();
        }
    }

    private static Customer mapOwner(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getString("customer_id"),
                rs.getString("customer_full_name"),
                rs.getString("customer_phone"));
    }

    /**
     * Маппинг ResultSet -> Account через {@link AccountFactory} (Factory Method).
     * Репозиторий не знает о конкретных классах счетов — только подготавливает
     * спецификацию и делегирует выбор реализации фабрике.
     */
    private static Account mapAccount(ResultSet rs, Customer owner) throws SQLException {
        String kind = rs.getString("account_kind");
        long lastInterestAccrualMs = rs.getLong("last_interest_accrual_ms");
        Instant lastInterestAccrualAt = rs.wasNull() ? null : Instant.ofEpochMilli(lastInterestAccrualMs);

        String rawInterestRate = rs.getString("interest_rate");
        String rawCreditLimit = rs.getString("credit_limit");
        String rawCurrentDebt = rs.getString("current_debt");

        AccountFactory.AccountSpec spec = new AccountFactory.AccountSpec(
                rs.getString("account_id"),
                rs.getString("number"),
                new BigDecimal(rs.getString("balance")),
                rs.getString("currency"),
                AccountStatus.valueOf(rs.getString("status")),
                owner,
                rawInterestRate == null ? null : new BigDecimal(rawInterestRate),
                lastInterestAccrualAt,
                rawCreditLimit == null ? null : new BigDecimal(rawCreditLimit),
                rawCurrentDebt == null ? null : new BigDecimal(rawCurrentDebt));

        return AccountFactory.createAccount(kind, spec);
    }
}
