package application.service;

import domain.model.User;
import domain.repository.UserRepository;
import java.nio.charset.StandardCharsets;//для превращения в байты
import java.security.MessageDigest;//содержит алгоритмы для хэширования
import java.security.NoSuchAlgorithmException;//обработчик ошибок в случае ненахождения алгоритма
import java.util.HexFormat;//превращение байтов хэша в 16ричную строку чтобы можно было занести в бд
import java.util.Optional;
import application.validation.AuthCredentialsValidator;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User registerUser(String login, String plainPassword, String customerId) {
        String phoneLogin = AuthCredentialsValidator.normalizeAndValidateLogin(login);
        AuthCredentialsValidator.validatePassword(plainPassword);

        userRepository.findByLogin(phoneLogin).ifPresent(existing -> {
            throw new IllegalArgumentException("User with this phone already exists");
        });

        String passwordHash = hashPassword(plainPassword);
        User user = new User(phoneLogin, passwordHash, customerId);
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
