package domain.model;

public class User {
    private final String login;
    private final String passwordHash;
    private final String customerId;

    public User(String login, String passwordHash, String customerId) {
        this.login = requireNonBlank(login, "User login is required");
        this.passwordHash = requireNonBlank(passwordHash, "Password hash is required");
        this.customerId = requireNonBlank(customerId, "Customer id is required");
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getCustomerId() {
        return customerId;
    }

    private static String requireNonBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}

