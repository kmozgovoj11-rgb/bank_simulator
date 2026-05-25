package infrastructure.persistence.sqlite;

import application.service.AuthService;
import application.service.BankService;
import domain.model.*;
import domain.repository.*;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class BankIntegrationTest {

    private Database database;
    private AccountRepository accountRepository;
    private CustomerRepository customerRepository;
    private TransactionRepository transactionRepository;
    private UserRepository userRepository;
    private TransactionBroker transactionBroker;

    private BankService bankService;
    private AuthService authService;

    private Path dbPath;

    @BeforeEach
    void setUp() throws Exception {
        dbPath = Path.of(System.getProperty("java.io.tmpdir"), "test-banksim-" + System.nanoTime() + ".db");
        database = new Database(dbPath);
        database.initializeSchema();

        accountRepository = new SqlAccountRepository(database);
        customerRepository = new SqlCustomerRepository(database);
        transactionRepository = new SqlTransactionRepository(database);
        userRepository = new SqlUserRepository(database);
        transactionBroker = new SqliteTransactionBroker(database);

        bankService = new BankService(accountRepository, customerRepository, transactionRepository, transactionBroker);
        authService = new AuthService(userRepository, customerRepository);
    }

    @AfterEach
    void tearDown() {
        dbPath.toFile().delete();
    }

    @Test
    @DisplayName("клиент → счёт → перевод → проверить балансы")
    void testFullScenario() {
        Customer alice = bankService.createCustomer("cust-1", "Alice", "+79001111111");
        assertNotNull(alice);

        DebitAccount acc1 = new DebitAccount("acc-1", "ACC-001", new BigDecimal("500.00"), "RUB", AccountStatus.ACTIVE, alice);
        DebitAccount acc2 = new DebitAccount("acc-2", "ACC-002", new BigDecimal("100.00"), "RUB", AccountStatus.ACTIVE, alice);
        accountRepository.save(acc1);
        accountRepository.save(acc2);

        TransferTransaction tx = bankService.transferMoney("ACC-001", "ACC-002", new BigDecimal("200.00"), "перевод 200");
        assertNotNull(tx);

        Account updated1 = accountRepository.findByNumber("ACC-001").orElseThrow();
        Account updated2 = accountRepository.findByNumber("ACC-002").orElseThrow();
        assertEquals(new BigDecimal("300.00"), updated1.getBalance());
        assertEquals(new BigDecimal("300.00"), updated2.getBalance());
    }

    @Test
    @DisplayName("история транзакций — несколько переводов")
    void testTransactionHistory() {
        Customer bob = bankService.createCustomer("cust-2", "Bob", "+79002222222");

        DebitAccount acc1 = new DebitAccount("acc-10", "ACC-010", new BigDecimal("1000.00"), "RUB", AccountStatus.ACTIVE, bob);
        DebitAccount acc2 = new DebitAccount("acc-20", "ACC-020", new BigDecimal("0.00"), "RUB", AccountStatus.ACTIVE, bob);
        accountRepository.save(acc1);
        accountRepository.save(acc2);

        bankService.transferMoney("ACC-010", "ACC-020", new BigDecimal("100.00"), "первый");
        bankService.transferMoney("ACC-010", "ACC-020", new BigDecimal("200.00"), "второй");
        bankService.transferMoney("ACC-010", "ACC-020", new BigDecimal("300.00"), "третий");

        List<Transaction> history = bankService.getAccountHistory("ACC-010");
        assertEquals(3, history.size());

        Account updated1 = accountRepository.findByNumber("ACC-010").orElseThrow();
        Account updated2 = accountRepository.findByNumber("ACC-020").orElseThrow();
        assertEquals(new BigDecimal("400.00"), updated1.getBalance());
        assertEquals(new BigDecimal("600.00"), updated2.getBalance());
    }

    @Test
    @DisplayName("регистрация и аутентификация")
    void testRegistrationAndLogin() {
        bankService.createCustomer("cust-99", "Charlie", "+79003333333");

        User user = authService.registerUser("+79003333333", "mypassword", "cust-99");
        assertNotNull(user);

        Optional<User> loggedIn = authService.authenticate("+79003333333", "mypassword");
        assertTrue(loggedIn.isPresent());

        Optional<User> wrong = authService.authenticate("+79003333333", "wrongpass");
        assertTrue(wrong.isEmpty());
    }

    @Test
    @DisplayName("перевод с несуществующего счёта — ошибка")
    void testTransferAccountNotFound() {
        assertThrows(IllegalArgumentException.class, () ->
            bankService.transferMoney("NO-SUCH", "FAKE", new BigDecimal("100.00"), "ошибка"));
    }

    @Test
    @DisplayName("перевод с закрытого счёта — ошибка")
    void testTransferFromClosedAccount() {
        Customer alice = bankService.createCustomer("cust-5", "Alice", "+79004444444");

        DebitAccount active = new DebitAccount("acc-30", "ACC-030", new BigDecimal("500.00"), "RUB", AccountStatus.ACTIVE, alice);
        DebitAccount closed = new DebitAccount("acc-31", "ACC-031", new BigDecimal("0.00"), "RUB", AccountStatus.CLOSED, alice);
        accountRepository.save(active);
        accountRepository.save(closed);

        assertThrows(IllegalStateException.class, () ->
            bankService.transferMoney("ACC-031", "ACC-030", new BigDecimal("50.00"), "не пройдёт"));
    }
}