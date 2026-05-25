package domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;

/**
 * Паттерн Factory Method: создание конкретного типа счёта по ключу {@code accountKind}.
 * Вызывающий код не зависит от классов DebitAccount/SavingsAccount/CreditAccount напрямую.
 * Используется в:
 *  - {@code SqlAccountRepository} — при чтении счетов из БД (поле {@code account_kind});
 *  - {@code CustomerPortalService} — при создании дополнительных счетов (SAVINGS/CREDIT) клиентом.
 *
 * Чтобы добавить новый тип счёта — достаточно добавить ветку switch здесь.
 */
public final class AccountFactory {
    private AccountFactory() {}

    /**
     * Спецификация для создания счёта.
     * Поля, не нужные для конкретного типа, могут быть {@code null}
     * (например, у дебетового счёта нет {@code interestRate} или {@code creditLimit}).
     */
    public record AccountSpec(
            String accountId,
            String number,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Customer owner,
            BigDecimal interestRate,
            Instant lastInterestAccrualAt,
            BigDecimal creditLimit,
            BigDecimal currentDebt) {}

    /**
     * Возвращает конкретный {@link Account} по значению {@code accountKind}.
     * Допустимые ключи: {@code DEBIT}, {@code SAVINGS}, {@code CREDIT} (без учёта регистра).
     */
    public static Account createAccount(String accountKind, AccountSpec spec) {
        if (accountKind == null || accountKind.isBlank()) {
            throw new IllegalArgumentException("Account kind is required");
        }
        String kind = accountKind.trim().toUpperCase(Locale.ROOT);
        return switch (kind) {
            case "DEBIT" -> new DebitAccount(
                    spec.accountId(),
                    spec.number(),
                    spec.balance(),
                    spec.currency(),
                    spec.status(),
                    spec.owner());
            case "SAVINGS" -> new SavingsAccount(
                    spec.accountId(),
                    spec.number(),
                    spec.balance(),
                    spec.currency(),
                    spec.status(),
                    spec.owner(),
                    spec.interestRate(),
                    spec.lastInterestAccrualAt());
            case "CREDIT" -> new CreditAccount(
                    spec.accountId(),
                    spec.number(),
                    spec.balance(),
                    spec.currency(),
                    spec.status(),
                    spec.owner(),
                    spec.creditLimit(),
                    spec.currentDebt());
            default -> throw new IllegalArgumentException("Unsupported account kind: " + accountKind);
        };
    }
}
