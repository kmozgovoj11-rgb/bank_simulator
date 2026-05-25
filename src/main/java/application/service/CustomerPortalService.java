package application.service;

import application.validation.AuthCredentialsValidator;
import domain.model.Account;
import domain.model.AccountFactory;
import domain.model.AccountStatus;
import domain.model.Customer;
import domain.model.DebitAccount;
import domain.model.DepositTransaction;
import domain.model.InterestAccrualTransaction;
import domain.model.SavingsAccount;
import domain.model.StoredTransaction;
import domain.model.Transaction;
import domain.model.TransferTransaction;
import domain.model.User;
import domain.model.WithdrawTransaction;
import domain.repository.AccountRepository;
import domain.repository.CustomerRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CustomerPortalService {
    private static final DateTimeFormatter ACCOUNT_SUFFIX =
            DateTimeFormatter.ofPattern("ddHHmm", Locale.ROOT).withZone(ZoneId.of("UTC"));

    // Зависим от фасада, а не от конкретной реализации — выгода паттерна Facade.
    private final BankFacade bankService;
    private final AuthService authService;
    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public CustomerPortalService(
            BankFacade bankService,
            AuthService authService,
            AccountRepository accountRepository,
            CustomerRepository customerRepository) {
        this.bankService = bankService;
        this.authService = authService;
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    public SessionResponse registerCustomer(String fullName, String phone, String password) {
        String customerId = UUID.randomUUID().toString();
        Customer customer = bankService.createCustomer(customerId, fullName, phone);
        authService.registerUser(phone, password, customerId);
        createStarterAccounts(customer);
        return new SessionResponse(toCustomerDto(customer));
    }

    public SessionResponse loginCustomer(String phone, String password) {
        User user = authService.authenticate(phone, password)
                .orElseThrow(() -> new IllegalArgumentException("Invalid phone or password"));
        Customer customer = customerRepository.findByCustomerId(user.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer profile not found"));
        return new SessionResponse(toCustomerDto(customer));
    }

    public DashboardResponse getDashboard(String customerId) {
        Customer customer = findCustomer(customerId);
        List<Account> accounts = accountRepository.findByCustomer(customerId);
        List<AccountDto> accountDtos = accounts.stream().map(this::toAccountDto).toList();

        List<TransactionDto> recentTransactions = accounts.stream()
                .flatMap(account -> bankService.getAccountHistory(account.getNumber()).stream())
                .map(this::toTransactionDto)
                .sorted(Comparator.comparing(TransactionDto::timestamp).reversed())
                .distinct()
                .limit(12)
                .toList();

        return new DashboardResponse(toCustomerDto(customer), accountDtos, recentTransactions);
    }

    public AccountHistoryResponse getAccountHistory(String accountNumber) {
        bankService.findAccount(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
        List<TransactionDto> transactions = bankService.getAccountHistory(accountNumber).stream()
                .map(this::toTransactionDto)
                .toList();
        return new AccountHistoryResponse(accountNumber, transactions);
    }

    public TransactionDto createTransfer(
            String customerId,
            String fromAccountNumber,
            String recipient,
            BigDecimal amount,
            String description) {
        Account fromAccount = findRequiredDebitSourceAccount(customerId, fromAccountNumber);
        Account recipientAccount = resolveRecipientStandardDebit(recipient);
        TransferTransaction transaction = bankService.transferMoney(
                fromAccount.getNumber(),
                recipientAccount.getNumber(),
                amount,
                description.isBlank() ? "Перевод клиенту" : description);
        return toTransactionDto(transaction);
    }

    public AccountDto createAccount(String customerId, String accountKind) {
        Customer customer = findCustomer(customerId);
        String normalizedKind = normalizeAccountKind(accountKind);

        // Создание счёта делегируется фабрике (Factory Method).
        // Здесь только готовим спецификацию для нужного типа.
        String numberPrefix = normalizedKind.equals("SAVINGS") ? "SVG-" : "CRD-";
        AccountFactory.AccountSpec spec = new AccountFactory.AccountSpec(
                UUID.randomUUID().toString(),
                numberPrefix + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT),
                new BigDecimal("0.00"),
                "RUB",
                AccountStatus.ACTIVE,
                customer,
                normalizedKind.equals("SAVINGS") ? new BigDecimal("0.08") : null,
                null,
                normalizedKind.equals("CREDIT") ? new BigDecimal("100000.00") : null,
                normalizedKind.equals("CREDIT") ? new BigDecimal("0.00") : null);

        Account account = AccountFactory.createAccount(normalizedKind, spec);

        customer.addAccount(account);
        accountRepository.save(account);
        return toAccountDto(account);
    }

    public AccountDto deleteAdditionalAccount(String customerId, String accountNumber) {
        Customer customer = findCustomer(customerId);
        String normalizedAccountNumber = normalizeAccountNumber(accountNumber);
        Account account = accountRepository.findByNumber(normalizedAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + normalizedAccountNumber));

        if (!account.getOwner().getCustomerId().equals(customer.getCustomerId())) {
            throw new IllegalArgumentException("Selected account does not belong to the current customer");
        }
        if (account instanceof DebitAccount) {
            throw new IllegalArgumentException("Standard debit account cannot be deleted");
        }

        account.close();
        accountRepository.delete(account.getAccountId());
        return toAccountDto(account);
    }

    public TransactionDto topUpStandardDebit(String customerId, BigDecimal amount, String description) {
        Account targetAccount = findStandardDebitAccount(findCustomer(customerId));
        DepositTransaction transaction = bankService.depositMoney(
                targetAccount.getNumber(),
                amount,
                description.isBlank() ? "Пополнение счета" : description);
        return toTransactionDto(transaction);
    }

    public TransactionDto fundSavingsFromStandardDebit(
            String customerId,
            String savingsAccountNumber,
            BigDecimal amount,
            String description) {
        Customer customer = findCustomer(customerId);
        Account fromAccount = findStandardDebitAccount(customer);
        Account targetAccount = bankService.findAccount(normalizeAccountNumber(savingsAccountNumber))
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + savingsAccountNumber));

        if (!targetAccount.getOwner().getCustomerId().equals(customer.getCustomerId())) {
            throw new IllegalArgumentException("Selected savings account does not belong to the current customer");
        }
        if (!(targetAccount instanceof SavingsAccount)) {
            throw new IllegalArgumentException("Savings top-up target must be a savings account");
        }

        TransferTransaction transaction = bankService.transferMoney(
                fromAccount.getNumber(),
                targetAccount.getNumber(),
                amount,
                description.isBlank() ? "Пополнение сберегательного счета" : description);
        return toTransactionDto(transaction);
    }

    public InterestAccrualResponse accrueInterest(String customerId, String accountNumber) {
        Account account = bankService.findAccount(normalizeAccountNumber(accountNumber))
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
        if (!account.getOwner().getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Selected account does not belong to the current customer");
        }
        if (!(account instanceof SavingsAccount)) {
            throw new IllegalArgumentException("Interest can be accrued only for savings accounts");
        }

        InterestAccrualTransaction transaction = bankService.accrueInterest(account.getNumber());
        SavingsAccount updatedAccount = transaction.getTargetAccount();
        return new InterestAccrualResponse(
                updatedAccount.getNumber(),
                transaction.getAmount().toPlainString(),
                updatedAccount.getBalance().toPlainString(),
                transaction.getTimestamp().toString(),
                toTransactionDto(transaction));
    }

    private void createStarterAccounts(Customer customer) {
        Account debit = new DebitAccount(
                UUID.randomUUID().toString(),
                "ACC-" + ACCOUNT_SUFFIX.format(Instant.now()),
                new BigDecimal("15000.00"),
                "RUB",
                AccountStatus.ACTIVE,
                customer);

        customer.addAccount(debit);
        accountRepository.save(debit);
    }

    private Customer findCustomer(String customerId) {
        return customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
    }

    private Account findRequiredDebitSourceAccount(String customerId, String fromAccountNumber) {
        Account fromAccount = bankService.findAccount(fromAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + fromAccountNumber));
        if (!(fromAccount instanceof DebitAccount)) {
            throw new IllegalArgumentException("Transfers can be sent only from a standard debit account");
        }
        if (!fromAccount.getOwner().getCustomerId().equals(customerId)) {
            throw new IllegalArgumentException("Selected account does not belong to the current customer");
        }
        return fromAccount;
    }

    private Account resolveRecipientStandardDebit(String recipient) {
        String trimmedRecipient = recipient == null ? "" : recipient.trim();
        if (trimmedRecipient.isEmpty()) {
            throw new IllegalArgumentException("Recipient phone or debit account number is required");
        }

        if (looksLikePhone(trimmedRecipient)) {
            String normalizedPhone = AuthCredentialsValidator.normalizeAndValidateLogin(trimmedRecipient);
            Customer customer = customerRepository.findByPhone(normalizedPhone)
                    .orElseThrow(() -> new IllegalArgumentException("Recipient with this phone was not found"));
            return findStandardDebitAccount(customer);
        }

        Account recipientAccount = bankService.findAccount(trimmedRecipient)
                .orElseThrow(() -> new IllegalArgumentException("Recipient account not found"));
        if (!(recipientAccount instanceof DebitAccount)) {
            throw new IllegalArgumentException("Transfers can only be received by a standard debit account");
        }
        return recipientAccount;
    }

    private boolean looksLikePhone(String value) {
        return value.startsWith("+") || value.chars().allMatch(Character::isDigit);
    }

    private Account findStandardDebitAccount(Customer customer) {
        return accountRepository.findByCustomer(customer.getCustomerId()).stream()
                .filter(DebitAccount.class::isInstance)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Customer does not have a standard debit account"));
    }

    private String normalizeAccountKind(String accountKind) {
        if (accountKind == null || accountKind.isBlank()) {
            throw new IllegalArgumentException("Account kind is required");
        }
        String normalized = accountKind.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("SAVINGS") && !normalized.equals("CREDIT")) {
            throw new IllegalArgumentException("Only SAVINGS and CREDIT accounts can be created");
        }
        return normalized;
    }

    private String normalizeAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }
        return accountNumber.trim();
    }

    private CustomerDto toCustomerDto(Customer customer) {
        return new CustomerDto(customer.getCustomerId(), customer.getFullName(), customer.getPhone());
    }

    private AccountDto toAccountDto(Account account) {
        String kind = account.getClass().getSimpleName().replace("Account", "").toUpperCase(Locale.ROOT);
        String interestRate = null;
        String lastInterestAccrualAt = null;
        Boolean canAccrueInterest = null;
        if (account instanceof SavingsAccount savingsAccount) {
            interestRate = savingsAccount.getInterestRate().toPlainString();
            if (savingsAccount.getLastInterestAccrualAt() != null) {
                lastInterestAccrualAt = savingsAccount.getLastInterestAccrualAt().toString();
            }
            canAccrueInterest = savingsAccount.canAccrueInterest(Instant.now());
        }
        return new AccountDto(
                account.getAccountId(),
                account.getNumber(),
                account.getBalance().toPlainString(),
                account.getCurrency(),
                account.getStatus().name(),
                kind,
                interestRate,
                lastInterestAccrualAt,
                canAccrueInterest);
    }

    private TransactionDto toTransactionDto(Transaction transaction) {
        String fromAccountNumber = null;
        String toAccountNumber = null;
        String currency = "RUB";

        if (transaction instanceof TransferTransaction transferTransaction) {
            fromAccountNumber = transferTransaction.getFromAccount().getNumber();
            toAccountNumber = transferTransaction.getToAccount().getNumber();
            currency = transferTransaction.getFromAccount().getCurrency();
        } else if (transaction instanceof WithdrawTransaction withdrawTransaction) {
            fromAccountNumber = withdrawTransaction.getSourceAccount().getNumber();
            currency = withdrawTransaction.getSourceAccount().getCurrency();
        } else if (transaction instanceof DepositTransaction depositTransaction) {
            toAccountNumber = depositTransaction.getTargetAccount().getNumber();
            currency = depositTransaction.getTargetAccount().getCurrency();
        } else if (transaction instanceof InterestAccrualTransaction interestAccrualTransaction) {
            toAccountNumber = interestAccrualTransaction.getTargetAccount().getNumber();
            currency = interestAccrualTransaction.getTargetAccount().getCurrency();
        } else if (transaction instanceof StoredTransaction storedTransaction) {
            fromAccountNumber = storedTransaction.getFromAccountNumber();
            toAccountNumber = storedTransaction.getToAccountNumber();
            currency = storedTransaction.getCurrency();
        }

        return new TransactionDto(
                transaction.getTransactionId(),
                transaction.getType(),
                transaction.getAmount().toPlainString(),
                currency,
                transaction.getTimestamp().toString(),
                transaction.getStatus().name(),
                transaction.getDescription(),
                fromAccountNumber,
                toAccountNumber);
    }

    public record SessionResponse(CustomerDto customer) {}

    public record DashboardResponse(
            CustomerDto customer,
            List<AccountDto> accounts,
            List<TransactionDto> recentTransactions) {}

    public record AccountHistoryResponse(String accountNumber, List<TransactionDto> transactions) {}

    public record InterestAccrualResponse(
            String accountNumber,
            String amount,
            String newBalance,
            String accruedAt,
            TransactionDto transaction) {}

    public record CustomerDto(String customerId, String fullName, String phone) {}

    public record AccountDto(
            String accountId,
            String number,
            String balance,
            String currency,
            String status,
            String kind,
            String interestRate,
            String lastInterestAccrualAt,
            Boolean canAccrueInterest) {}

    public record TransactionDto(
            String transactionId,
            String type,
            String amount,
            String currency,
            String timestamp,
            String status,
            String description,
            String fromAccountNumber,
            String toAccountNumber) {}
}
