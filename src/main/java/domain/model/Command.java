package domain.model;

/**
 * Паттерн Command: представляет банковское действие как самостоятельный объект.
 * Объект умеет себя выполнить и откатить, что отделяет инициатора операции от её реализации.
 * Реализуется абстрактным классом {@link Transaction} и его конкретными подклассами
 * (DepositTransaction, WithdrawTransaction, TransferTransaction, InterestAccrualTransaction).
 */
public interface Command {
    /** Выполнить операцию. */
    void execute();

    /** Откатить ранее выполненную операцию. */
    void rollback();
}
