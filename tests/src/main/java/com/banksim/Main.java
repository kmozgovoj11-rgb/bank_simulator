package com.banksim;

import com.banksim.application.service.AuthService;
import com.banksim.application.service.BankService;
import com.banksim.domain.model.AccountStatus;
import com.banksim.domain.model.Customer;
import com.banksim.domain.model.DebitAccount;
import com.banksim.domain.repository.AccountRepository;
import com.banksim.infrastructure.persistence.sqlite.Database;
import com.banksim.infrastructure.persistence.sqlite.SqlAccountRepository;
import com.banksim.infrastructure.persistence.sqlite.SqlCustomerRepository;
import com.banksim.infrastructure.persistence.sqlite.SqliteTransactionBroker;
import com.banksim.infrastructure.persistence.sqlite.SqlTransactionRepository;
import com.banksim.infrastructure.persistence.sqlite.SqlUserRepository;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Minimal composition root: creates SQLite DB file, schema, and wired services.
 * Run: {@code mvn -q exec:java} from project root.
 */
public final class Main {
    private Main() {}

    public static void main(String[] args) throws SQLException {
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

        demoIfEmpty(bankService, authService, accountRepository, database);
        System.out.println("Bank simulator ready. DB: " + dbPath.toAbsolutePath());
    }

    private static void demoIfEmpty(
            BankService bankService,
            AuthService authService,
            AccountRepository accountRepository,
            Database database)
            throws SQLException {
        try (var c = database.openConnection();
                var ps = c.prepareStatement("SELECT COUNT(*) FROM customers");
                var rs = ps.executeQuery()) {
            if (rs.next() && rs.getInt(1) > 0) {
                return;
            }
        }

        Customer alice = bankService.createCustomer("cust-1", "Alice", "+1000000000");
        authService.registerUser("+79001234567", "secret12", alice.getCustomerId());

        DebitAccount acc1 =
                new DebitAccount(UUID.randomUUID().toString(), "ACC-001", new BigDecimal("100.00"), "USD", AccountStatus.ACTIVE, alice);
        DebitAccount acc2 =
                new DebitAccount(UUID.randomUUID().toString(), "ACC-002", new BigDecimal("50.00"), "USD", AccountStatus.ACTIVE, alice);
        alice.addAccount(acc1);
        alice.addAccount(acc2);
        accountRepository.save(acc1);
        accountRepository.save(acc2);

        bankService.transferMoney("ACC-001", "ACC-002", new BigDecimal("25.00"), "demo transfer");
    }
}
