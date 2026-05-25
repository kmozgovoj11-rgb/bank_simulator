package com.banksim.infrastructure.persistence.sqlite;

import java.sql.Connection;

final class SqliteConnectionContext {
    private static final ThreadLocal<Connection> CURRENT = new ThreadLocal<>();

    private SqliteConnectionContext() {}

    static Connection get() {
        return CURRENT.get();
    }

    static void set(Connection connection) {
        CURRENT.set(connection);
    }

    static void clear() {
        CURRENT.remove();
    }
}
