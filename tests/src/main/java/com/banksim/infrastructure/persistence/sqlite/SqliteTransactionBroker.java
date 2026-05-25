package com.banksim.infrastructure.persistence.sqlite;

import com.banksim.domain.repository.TransactionBroker;
import java.sql.Connection;
import java.sql.SQLException;

public final class SqliteTransactionBroker implements TransactionBroker {
    private final Database database;

    public SqliteTransactionBroker(Database database) {
        this.database = database;
    }

    @Override
    public void inTransaction(Runnable action) {
        if (SqliteConnectionContext.get() != null) {
            action.run();
            return;
        }
        try (Connection connection = database.openConnection()) {
            connection.setAutoCommit(false);
            SqliteConnectionContext.set(connection);
            try {
                action.run();
                connection.commit();
            } catch (RuntimeException e) {
                safeRollback(connection);
                throw e;
            } finally {
                SqliteConnectionContext.clear();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Transaction failed", e);
        }
    }

    private static void safeRollback(Connection connection) {
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // best-effort rollback
        }
    }
}
