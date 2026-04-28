package infrastructure.persistence.sqlite;

import domain.model.Customer;
import domain.repository.CustomerRepository;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Optional;

public class SqlCustomerRepository implements CustomerRepository {
    private final Database database;

    public SqlCustomerRepository(Database database) {
        this.database = Objects.requireNonNull(database);
    }

    @Override
    public Optional<Customer> findByCustomerId(String customerId) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                return findByCustomerId(ctx, customerId);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        }
        try (Connection connection = database.openConnection()) {
            return findByCustomerId(connection, customerId);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Optional<Customer> findByCustomerId(Connection connection, String customerId) throws SQLException {
        try (PreparedStatement ps =
                connection.prepareStatement("SELECT customer_id, full_name, phone FROM customers WHERE customer_id = ?")) {
            ps.setString(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(mapRow(rs));
            }
        }
    }

    @Override
    public void save(Customer customer) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                save(ctx, customer);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return;
        }
        try (Connection connection = database.openConnection()) {
            save(connection, customer);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void save(Connection connection, Customer customer) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(
                """
                INSERT INTO customers (customer_id, full_name, phone)
                VALUES (?, ?, ?)
                ON CONFLICT(customer_id) DO UPDATE SET
                    full_name = excluded.full_name,
                    phone = excluded.phone
                """)) {
            ps.setString(1, customer.getCustomerId());
            ps.setString(2, customer.getFullName());
            ps.setString(3, customer.getPhone());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(String customerId) {
        Connection ctx = SqliteConnectionContext.get();
        if (ctx != null) {
            try {
                delete(ctx, customerId);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
            return;
        }
        try (Connection connection = database.openConnection()) {
            delete(connection, customerId);
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void delete(Connection connection, String customerId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM customers WHERE customer_id = ?")) {
            ps.setString(1, customerId);
            ps.executeUpdate();
        }
    }

    static Customer mapRow(ResultSet rs) throws SQLException {
        return new Customer(rs.getString("customer_id"), rs.getString("full_name"), rs.getString("phone"));
    }
}
