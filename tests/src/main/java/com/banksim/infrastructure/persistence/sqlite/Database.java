package com.banksim.infrastructure.persistence.sqlite;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private final String jdbcUrl;

    public Database(Path databaseFile) {
        try {
            Path parent = databaseFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create database directory", e);
        }
        String normalized = databaseFile.toAbsolutePath().toString().replace('\\', '/');
        this.jdbcUrl = "jdbc:sqlite:" + normalized;
    }

    public static Database openDefault() {
        Path dataDir = Path.of(System.getProperty("user.dir")).resolve("data");
        return new Database(dataDir.resolve("banksim.db"));
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public Connection openConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement pragma = connection.createStatement()) {
            pragma.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public void initializeSchema() throws SQLException {
        try (Connection connection = openConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS customers (
                        customer_id TEXT PRIMARY KEY,
                        full_name TEXT NOT NULL,
                        phone TEXT NOT NULL
                    )
                    """);
            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS users (
                        login TEXT PRIMARY KEY,
                        password_hash TEXT NOT NULL,
                        customer_id TEXT NOT NULL REFERENCES customers(customer_id) ON DELETE CASCADE
                    )
                    """);
            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS accounts (
                        account_id TEXT PRIMARY KEY,
                        number TEXT NOT NULL UNIQUE,
                        customer_id TEXT NOT NULL REFERENCES customers(customer_id) ON DELETE CASCADE,
                        account_kind TEXT NOT NULL,
                        balance TEXT NOT NULL,
                        currency TEXT NOT NULL,
                        status TEXT NOT NULL,
                        interest_rate TEXT,
                        credit_limit TEXT,
                        current_debt TEXT
                    )
                    """);
            statement.execute(
                    """
                    CREATE TABLE IF NOT EXISTS transactions (
                        transaction_id TEXT PRIMARY KEY,
                        type TEXT NOT NULL,
                        amount TEXT NOT NULL,
                        currency TEXT NOT NULL,
                        timestamp_ms INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        description TEXT,
                        from_account_number TEXT,
                        to_account_number TEXT
                    )
                    """);
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_from ON transactions(from_account_number)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_transactions_to ON transactions(to_account_number)");
        }
    }
}
