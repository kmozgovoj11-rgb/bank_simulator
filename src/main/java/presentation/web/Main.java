package presentation.web;

import application.service.AuthService;
import application.service.BankService;
import application.service.CustomerPortalService;
import domain.model.Account;
import domain.model.AccountStatus;
import domain.model.Customer;
import domain.model.DebitAccount;
import domain.model.User;
import domain.repository.AccountRepository;
import domain.repository.CustomerRepository;
import domain.repository.UserRepository;
import infrastructure.persistence.sqlite.Database;
import infrastructure.persistence.sqlite.SqlAccountRepository;
import infrastructure.persistence.sqlite.SqlCustomerRepository;
import infrastructure.persistence.sqlite.SqlTransactionRepository;
import infrastructure.persistence.sqlite.SqlUserRepository;
import infrastructure.persistence.sqlite.SqliteTransactionBroker;
import presentation.http.ApiServer;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.UUID;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws SQLException, IOException {
        Path dbPath = Path.of(System.getProperty("user.dir")).resolve("data").resolve("banksim.db");
        Database database = new Database(dbPath);
        database.initializeSchema();

        var accountRepository = new SqlAccountRepository(database);
        var customerRepository = new SqlCustomerRepository(database);
        var transactionRepository = new SqlTransactionRepository(database);
        var userRepository = new SqlUserRepository(database);
        var transactionBroker = new SqliteTransactionBroker(database);

        var bankService = new BankService(
                accountRepository, customerRepository, transactionRepository, transactionBroker);
        var authService = new AuthService(userRepository, customerRepository);
        var portalService =
                new CustomerPortalService(bankService, authService, accountRepository, customerRepository);

        ensureDemoData(bankService, accountRepository, customerRepository, userRepository);
        var apiServer = new ApiServer(portalService, 8080);
        apiServer.start();

        System.out.println("Bank simulator ready. DB: " + dbPath.toAbsolutePath());
        System.out.println("HTTP API ready at http://localhost:8080");
        System.out.println("Demo login: +79001234567 / secret12");
        System.out.println("Demo recipient phone: +79005556677");
    }

    private static void ensureDemoData(
            BankService bankService,
            AccountRepository accountRepository,
            CustomerRepository customerRepository,
            UserRepository userRepository) {
        Customer demoCustomer = new Customer("demo-customer", "Alice Carter", "+79001234567");
        customerRepository.save(demoCustomer);
        userRepository.save(new User("+79001234567", hashPassword("secret12"), demoCustomer.getCustomerId()));
        ensureDemoAccount(accountRepository, demoCustomer, "DEMO-001", new BigDecimal("120000.00"));
        removeLegacySecondDebit(accountRepository, demoCustomer.getCustomerId(), "DEMO-002");

        Customer recipientCustomer = new Customer("demo-recipient", "Bob Stone", "+79005556677");
        customerRepository.save(recipientCustomer);
        userRepository.save(new User("+79005556677", hashPassword("secret12"), recipientCustomer.getCustomerId()));
        ensureDemoAccount(accountRepository, recipientCustomer, "DEMO-201", new BigDecimal("35000.00"));
    }

    private static void ensureDemoAccount(
            AccountRepository accountRepository,
            Customer owner,
            String number,
            BigDecimal minimumBalance) {
        Account existing = accountRepository.findByNumber(number).orElse(null);
        if (existing == null) {
            DebitAccount created =
                    new DebitAccount(
                            UUID.randomUUID().toString(),
                            number,
                            minimumBalance,
                            "RUB",
                            AccountStatus.ACTIVE,
                            owner);
            owner.addAccount(created);
            accountRepository.save(created);
            return;
        }

        if (existing.getBalance().compareTo(minimumBalance) < 0 && existing.isActive()) {
            existing.deposit(minimumBalance.subtract(existing.getBalance()));
            accountRepository.save(existing);
        }
    }

    private static void removeLegacySecondDebit(
            AccountRepository accountRepository,
            String customerId,
            String number) {
        accountRepository.findByNumber(number)
                .filter(account -> account.getOwner().getCustomerId().equals(customerId))
                .ifPresent(account -> accountRepository.delete(account.getAccountId()));
    }

    private static String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
