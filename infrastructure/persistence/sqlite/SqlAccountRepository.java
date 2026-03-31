package infrastructure.persistence.sqlite;

import domain.model.Account;
import domain.model.AccountStatus;
import domain.model.CreditAccount;
import domain.model.Customer;
import domain.model.DebitAccount;
import domain.model.SavingsAccount;
import domain.repository.AccountRepository;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class SqlAccountRepository implements AccountRepository {
    private static final String SELECT_BASE =
            """
            SELECT a.account_id, a.number, a.customer_id, a.account_kind, a.balance, a.currency, a.status,
                   a.interest_rate, a.credit_limit, a.current_debt,
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
                return Optional.of(mapAccount(rs, owner));
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
        String creditLimit = null;
        String currentDebt = null;
        if (account instanceof SavingsAccount savingsAccount) {
            interestRate = savingsAccount.getInterestRate().toPlainString();
        } else if (account instanceof CreditAccount creditAccount) {
            creditLimit = creditAccount.getCreditLimit().toPlainString();
            currentDebt = creditAccount.getCurrentDebt().toPlainString();
        }

        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO accounts (
                    account_id, number, customer_id, account_kind, balance, currency, status,
                    interest_rate, credit_limit, current_debt
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(account_id) DO UPDATE SET
                    number = excluded.number,
                    customer_id = excluded.customer_id,
                    account_kind = excluded.account_kind,
                    balance = excluded.balance,
                    currency = excluded.currency,
                    status = excluded.status,
                    interest_rate = excluded.interest_rate,
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
            ps.setString(9, creditLimit);
            ps.setString(10, currentDebt);
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
                Customer owner = null;
                List<Account> accounts = new ArrayList<>();
                while (rs.next()) {
                    if (owner == null) {
                        owner = mapOwner(rs);
                    }
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

    private static Account mapAccount(ResultSet rs, Customer owner) throws SQLException {
        String accountId = rs.getString("account_id");
        String number = rs.getString("number");
        BigDecimal balance = new BigDecimal(rs.getString("balance"));
        String currency = rs.getString("currency");
        AccountStatus status = AccountStatus.valueOf(rs.getString("status"));
        String kind = rs.getString("account_kind");

        return switch (kind) {
            case "DEBIT" -> new DebitAccount(accountId, number, balance, currency, status, owner);
            case "SAVINGS" -> new SavingsAccount(
                    accountId,
                    number,
                    balance,
                    currency,
                    status,
                    owner,
                    new BigDecimal(rs.getString("interest_rate")));
            case "CREDIT" -> new CreditAccount(
                    accountId,
                    number,
                    balance,
                    currency,
                    status,
                    owner,
                    new BigDecimal(rs.getString("credit_limit")),
                    new BigDecimal(rs.getString("current_debt")));
            default -> throw new IllegalStateException("Unknown account_kind in DB: " + kind);
        };
    }
}