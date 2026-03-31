package infrastructure.persistence.sqlite;

import domain.model.DepositTransaction;
import domain.model.StoredTransaction;
import domain.model.Transaction;
import domain.model.TransactionStatus;
import domain.model.TransferTransaction;
import domain.model.WithdrawTransaction;
import domain.repository.TransactionRepository;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SqlTransactionRepository implements TransactionRepository {
    private final Database database;

    public SqlTransactionRepository(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public void save(Transaction transaction) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                save(ctx, transaction);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return;
        }
        try (Connection connection = database.openConnection()) {
            save(connection, transaction);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void save(Connection connection, Transaction transaction) throws SQLException {
        String fromNumber;
        String toNumber;
        String currency;

        if (transaction instanceof TransferTransaction transferTransaction) {
            fromNumber = transferTransaction.getFromAccount().getNumber();
            toNumber = transferTransaction.getToAccount().getNumber();
            currency = transferTransaction.getFromAccount().getCurrency();
        } else if (transaction instanceof DepositTransaction depositTransaction) {
            fromNumber = null;
            toNumber = depositTransaction.getTargetAccount().getNumber();
            currency = depositTransaction.getTargetAccount().getCurrency();
        } else if (transaction instanceof WithdrawTransaction withdrawTransaction) {
            fromNumber = withdrawTransaction.getSourceAccount().getNumber();
            toNumber = null;
            currency = withdrawTransaction.getSourceAccount().getCurrency();
        } else if (transaction instanceof StoredTransaction storedTransaction) {
            fromNumber = storedTransaction.getFromAccountNumber();
            toNumber = storedTransaction.getToAccountNumber();
            currency = storedTransaction.getCurrency();
        } else {
            throw new IllegalArgumentException(
                    "Unsupported transaction type: " + transaction.getClass().getName());
        }

        long timestampMs = transaction.getTimestamp().toEpochMilli();

        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO transactions (
                    transaction_id, type, amount, currency, timestamp_ms, status, description,
                    from_account_number, to_account_number
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT(transaction_id) DO UPDATE SET
                    type = excluded.type,
                    amount = excluded.amount,
                    currency = excluded.currency,
                    timestamp_ms = excluded.timestamp_ms,
                    status = excluded.status,
                    description = excluded.description,
                    from_account_number = excluded.from_account_number,
                    to_account_number = excluded.to_account_number
                """)) {
            ps.setString(1, transaction.getTransactionId());
            ps.setString(2, transaction.getType());
            ps.setString(3, transaction.getAmount().toPlainString());
            ps.setString(4, currency);
            ps.setLong(5, timestampMs);
            ps.setString(6, transaction.getStatus().name());
            ps.setString(7, transaction.getDescription());
            ps.setString(8, fromNumber);
            ps.setString(9, toNumber);
            ps.executeUpdate();
        }
    }

    @Override
    public List<Transaction> findByAccountNumber(String accountNumber) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                return findByAccountNumber(ctx, accountNumber);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
        try (Connection connection = database.openConnection()) {
            return findByAccountNumber(connection, accountNumber);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Transaction> findByAccountNumber(Connection connection, String accountNumber)
            throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                SELECT transaction_id, type, amount, currency, timestamp_ms, status, description,
                       from_account_number, to_account_number
                FROM transactions
                WHERE from_account_number = ? OR to_account_number = ?
                ORDER BY timestamp_ms DESC
                """)) {
            ps.setString(1, accountNumber);
            ps.setString(2, accountNumber);
            try (ResultSet rs = ps.executeQuery()) {
                List<Transaction> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return List.copyOf(result);
            }
        }
    }

    private static StoredTransaction mapRow(ResultSet rs) throws SQLException {
        return new StoredTransaction(
                rs.getString("transaction_id"),
                rs.getString("type"),
                new BigDecimal(rs.getString("amount")),
                Instant.ofEpochMilli(rs.getLong("timestamp_ms")),
                TransactionStatus.valueOf(rs.getString("status")),
                rs.getString("description"),
                rs.getString("currency"),
                rs.getString("from_account_number"),
                rs.getString("to_account_number"));
    }
}
