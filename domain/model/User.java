package domain.model;

public class User {
    private final String login;
    private final String passwordHash;
    private final String customerId;

    public User(String login, String passwordHash, String customerId) {
        this.login = login;
        this.passwordHash = passwordHash;
        this.customerId = customerId;
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
}

