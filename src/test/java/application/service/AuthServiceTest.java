package application.service;

import domain.model.User;
import domain.repository.CustomerRepository;
import domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, customerRepository);
    }

    @Test
    @DisplayName("Регистрация пользователя — успешно")
    void testRegisterUserSuccess() {
        when(userRepository.findByLogin("+79001234567")).thenReturn(Optional.empty());
        when(customerRepository.findByCustomerId("cust-1"))
                .thenReturn(Optional.of(new domain.model.Customer("cust-1", "Alice", "+79001234567")));

        User user = authService.registerUser("+79001234567", "secret123", "cust-1");

        assertNotNull(user);
        assertEquals("+79001234567", user.getLogin());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Регистрация — телефон уже занят")
    void testRegisterUserDuplicatePhone() {
        when(userRepository.findByLogin("+79001234567"))
                .thenReturn(Optional.of(new User("+79001234567", "hash", "cust-99")));

        assertThrows(IllegalArgumentException.class, () ->
                authService.registerUser("+79001234567", "secret123", "cust-1"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Регистрация — клиент не найден")
    void testRegisterUserCustomerNotFound() {
        when(userRepository.findByLogin("+79001234567")).thenReturn(Optional.empty());
        when(customerRepository.findByCustomerId("cust-1")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                authService.registerUser("+79001234567", "secret123", "cust-1"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Регистрация — короткий пароль")
    void testRegisterUserShortPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.registerUser("+79001234567", "12345", "cust-1"));
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Регистрация — пароль null")
    void testRegisterUserNullPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.registerUser("+79001234567", null, "cust-1"));
    }

    @Test
    @DisplayName("Регистрация — пустой customerId")
    void testRegisterUserEmptyCustomerId() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.registerUser("+79001234567", "secret123", "   "));
    }

    @Test
    @DisplayName("Аутентификация — пользователь не найден")
    void testAuthenticateUserNotFound() {
        when(userRepository.findByLogin("+79001234567")).thenReturn(Optional.empty());
        Optional<User> result = authService.authenticate("+79001234567", "secret123");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Аутентификация — неверный пароль")
    void testAuthenticateWrongPassword() {
        User existingUser = new User("+79001234567", "somehash", "cust-1");
        when(userRepository.findByLogin("+79001234567")).thenReturn(Optional.of(existingUser));
        Optional<User> result = authService.authenticate("+79001234567", "wrongpass");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Аутентификация — пустой логин")
    void testAuthenticateEmptyLogin() {
        assertThrows(IllegalArgumentException.class, () ->
                authService.authenticate("   ", "secret123"));
    }
}