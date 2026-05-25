package application.service;

import domain.model.*;
import domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionBroker transactionBroker;

    private BankService bankService;

    private Customer testCustomer;
    private DebitAccount fromAccount;
    private DebitAccount toAccount;

    @BeforeEach
    void setUp() {
        bankService = new BankService(accountRepository, customerRepository, transactionRepository, transactionBroker);

        testCustomer = new Customer("cust-1", "Alice", "+79000000001");

        fromAccount = new DebitAccount("acc-1", "ACC-001", new BigDecimal("200.00"), "USD", AccountStatus.ACTIVE, testCustomer);
        toAccount = new DebitAccount("acc-2", "ACC-002", new BigDecimal("50.00"), "USD", AccountStatus.ACTIVE, testCustomer);
    }

    private void mockTransactionBroker() {
        doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(transactionBroker).inTransaction(any());
    }

    @Test
    @DisplayName("Создание клиента — успешно")
    void testCreateCustomer() {
        bankService.createCustomer("cust-1", "Alice", "+79000000001");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Создание клиента с пустым именем — ошибка")
    void testCreateCustomerEmptyName() {
        assertThrows(IllegalArgumentException.class, () -> bankService.createCustomer("cust-1", "", "+79000000001"));
        verify(customerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод денег между счетами — успешно")
    void testTransferMoney() {
        mockTransactionBroker();
        when(accountRepository.findByNumber("ACC-001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByNumber("ACC-002")).thenReturn(Optional.of(toAccount));

        TransferTransaction result = bankService.transferMoney("ACC-001", "ACC-002", new BigDecimal("100.00"), "Test");

        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), fromAccount.getBalance());
        assertEquals(new BigDecimal("150.00"), toAccount.getBalance());
        verify(transactionRepository).save(any(TransferTransaction.class));
        verify(accountRepository, times(2)).save(any(Account.class));
    }

    @Test
    @DisplayName("Перевод с несуществующего счёта — ошибка")
    void testTransferMoneyFromAccountNotFound() {
        mockTransactionBroker();
        when(accountRepository.findByNumber("ACC-001")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            bankService.transferMoney("ACC-001", "ACC-002", new BigDecimal("100.00"), "Test"));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Перевод на несуществующий счёт — ошибка")
    void testTransferMoneyToAccountNotFound() {
        mockTransactionBroker();
        when(accountRepository.findByNumber("ACC-001")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByNumber("ACC-002")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
            bankService.transferMoney("ACC-001", "ACC-002", new BigDecimal("100.00"), "Test"));
    }

    @Test
    @DisplayName("Поиск счёта по номеру")
    void testFindAccount() {
        when(accountRepository.findByNumber("ACC-001")).thenReturn(Optional.of(fromAccount));

        Optional<Account> result = bankService.findAccount("ACC-001");

        assertTrue(result.isPresent());
        assertEquals("ACC-001", result.get().getNumber());
    }
}