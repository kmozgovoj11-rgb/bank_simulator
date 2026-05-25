package com.banksim.domain.model;

import java.math.BigDecimal;

/**
 * Кредитный счёт: помимо баланса учитывается лимит и текущий долг.
 * Снятие сначала тратит баланс, недостающее уходит в долг (в пределах лимита).
 */
public class CreditAccount extends Account {
    private final BigDecimal creditLimit;
    private BigDecimal currentDebt;

    public CreditAccount(
            String accountId,
            String number,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Customer owner,
            BigDecimal creditLimit,
            BigDecimal currentDebt) {
        super(accountId, number, balance, currency, status, owner);
        this.creditLimit = requireNonNegative(creditLimit, "Credit limit");
        this.currentDebt = requireNonNegative(currentDebt, "Current debt");
    }

    @Override
    public void withdraw(BigDecimal amount) {
        ensureAccountAllowsDepositsAndWithdrawals();
        validatePositiveAmount(amount);
        // Доступно «наличными» + неиспользованный лимит (уже занятый долг уменьшает доступное).
        BigDecimal totalAvailable = getBalance().add(creditLimit).subtract(currentDebt);
        if (totalAvailable.compareTo(amount) < 0) {
            throw new IllegalStateException("Credit limit exceeded");
        }
        if (getBalance().compareTo(amount) >= 0) {
            super.withdraw(amount);
        } else {
            // Часть с баланса, остаток — увеличение долга (super.withdraw обнуляет баланс до нуля).
            BigDecimal remaining = amount.subtract(getBalance());
            super.withdraw(getBalance());
            currentDebt = currentDebt.add(remaining);
        }
    }

    /**
     * Погашение долга: сумма списывается с долга; если платёж больше долга, излишек зачисляется на баланс через {@link #deposit}.
     */
    public void repayDebt(BigDecimal amount) {
        validatePositiveAmount(amount);
        if (currentDebt.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        if (currentDebt.compareTo(amount) <= 0) {
            BigDecimal excess = amount.subtract(currentDebt);
            currentDebt = BigDecimal.ZERO;
            if (excess.compareTo(BigDecimal.ZERO) > 0) {
                deposit(excess);
            }
            return;
        }
        currentDebt = currentDebt.subtract(amount);
    }

    @Override
    protected void ensureCanBeClosed() {
        super.ensureCanBeClosed();
        if (currentDebt.compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Credit account can be closed only with zero debt");
        }
    }

    @Override
    protected void rollbackWithdraw(BigDecimal amount) {
        // Зеркалит withdraw: приоритетно уменьшаем долг, остаток — через откат по балансу базового счёта.
        validatePositiveAmount(amount);
        if (currentDebt.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal debtReduction = currentDebt.min(amount);
            currentDebt = currentDebt.subtract(debtReduction);
            BigDecimal remaining = amount.subtract(debtReduction);
            if (remaining.compareTo(BigDecimal.ZERO) > 0) {
                super.rollbackWithdraw(remaining);
            }
            return;
        }
        super.rollbackWithdraw(amount);
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public BigDecimal getCurrentDebt() {
        return currentDebt;
    }
}
