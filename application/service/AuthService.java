package application.service;

import application.validation.AuthCredentialsValidator;
import domain.model.User;
import domain.repository.CustomerRepository;
import domain.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

public class AuthService {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public AuthService(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    public User registerUser(String login, String plainPassword, String customerId) {
        String phoneLogin = AuthCredentialsValidator.normalizeAndValidateLogin(login);
        AuthCredentialsValidator.validatePassword(plainPassword);
        if (customerId == null || customerId.isBlank()) {
            throw new IllegalArgumentException("Customer id is required");
        }

        userRepository.findByLogin(phoneLogin).ifPresent(existing -> {
            throw new IllegalArgumentException("User with this phone already exists");
        });
        customerRepository.findByCustomerId(customerId.trim()).orElseThrow(
                () -> new IllegalArgumentException("Customer not found: " + customerId));

        String passwordHash = hashPassword(plainPassword);
        User user = new User(phoneLogin, passwordHash, customerId.trim());
        userRepository.save(user);
        return user;
    }

    public Optional<User> authenticate(String login, String plainPassword) {
        String phoneLogin = AuthCredentialsValidator.normalizeAndValidateLogin(login);
        AuthCredentialsValidator.validatePasswordForLoginAttempt(plainPassword);

        String passwordHash = hashPassword(plainPassword);
        return userRepository.findByLogin(phoneLogin)
                .filter(user -> user.getPasswordHash().equals(passwordHash));
    }

    private String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}

