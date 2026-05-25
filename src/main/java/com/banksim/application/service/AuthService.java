package com.banksim.application.service;

import com.banksim.application.validation.AuthCredentialsValidator;
import com.banksim.domain.model.User;
import com.banksim.domain.repository.CustomerRepository;
import com.banksim.domain.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Регистрация и аутентификация пользователей по телефону (логин) и паролю.
 * <p>
 * Логин всегда приводится к каноническому виду через {@link AuthCredentialsValidator#normalizeAndValidateLogin(String)}
 * и совпадает с тем, как сохраняется телефон клиента при {@link BankService#createCustomer(String, String, String)}.
 * Пароль в хранилище не лежит в открытом виде: сохраняется и сравнивается SHA-256 в шестнадцатеричном виде (см. {@link #hashPassword}).
 */
public class AuthService {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;

    public AuthService(UserRepository userRepository, CustomerRepository customerRepository) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Создаёт учётную запись, если логин (телефон) ещё не занят и указанный клиент уже есть в системе.
     * Порядок проверок: нормализация логина и пароля → уникальность телефона → существование клиента → хеш и сохранение.
     */
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

    /**
     * Проверяет пароль: хеш введённого значения сравнивается с сохранённым.
     * При неверном пароле или отсутствии пользователя возвращается пустой {@link Optional} без различия причин (единый ответ снаружи).
     */
    public Optional<User> authenticate(String login, String plainPassword) {
        String phoneLogin = AuthCredentialsValidator.normalizeAndValidateLogin(login);
        AuthCredentialsValidator.validatePasswordForLoginAttempt(plainPassword);

        String passwordHash = hashPassword(plainPassword);
        return userRepository.findByLogin(phoneLogin)
                .filter(user -> user.getPasswordHash().equals(passwordHash));
    }

    /** SHA-256 от UTF-8 строки пароля, нижний регистр hex — для простого сравнения с полем в БД. */
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
