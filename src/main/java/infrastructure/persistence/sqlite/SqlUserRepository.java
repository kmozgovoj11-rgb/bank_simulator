package infrastructure.persistence.sqlite;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

import domain.model.User;
import domain.repository.UserRepository;

public class SqlUserRepository implements UserRepository {
    private final Database database;

    public SqlUserRepository(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public Optional<User> findByLogin(String login) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                return findByLogin(ctx, login);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
        try (Connection connection = database.openConnection()) {
            return findByLogin(connection, login);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Optional<User> findByLogin(Connection connection, String login) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT login, password_hash, customer_id FROM users WHERE login = ?")) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new User(
                        rs.getString("login"),
                        rs.getString("password_hash"),
                        rs.getString("customer_id")));
            }
        }
    }

    @Override
    public void save(User user) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                save(ctx, user);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return;
        }
        try (Connection connection = database.openConnection()) {
            save(connection, user);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void save(Connection connection, User user) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO users (login, password_hash, customer_id)
                VALUES (?, ?, ?)
                ON CONFLICT(login) DO UPDATE SET
                    password_hash = excluded.password_hash,
                    customer_id = excluded.customer_id
                """)) {
            ps.setString(1, user.getLogin());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getCustomerId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String login) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                delete(ctx, login);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return;
        }
        try (Connection connection = database.openConnection()) {
            delete(connection, login);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void delete(Connection connection, String login) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM users WHERE login = ?")) {
            ps.setString(1, login);
            ps.executeUpdate();
        }
    }
}

