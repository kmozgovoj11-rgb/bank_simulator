package domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class DebitAccountTest {

    private Customer createTestCustomer() {
        return new Customer("cust-1", "Alice", "+79000000001");
    }

    private DebitAccount createTestAccount() {
        Customer owner = createTestCustomer();
        return new DebitAccount("acc-1", "ACC-001", new BigDecimal("100.00"), "USD", AccountStatus.ACTIVE, owner);
    }

    @Test
    @DisplayName("Создание дебетового счёта")
    void testCreateDebitAccount() {
        DebitAccount account = createTestAccount();
        assertNotNull(account);
        assertEquals("ACC-001", account.getNumber());
        assertEquals(new BigDecimal("100.00"), account.getBalance());
        assertEquals("USD", account.getCurrency());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    @DisplayName("Пополнение счёта")
    void testDeposit() {
        DebitAccount account = createTestAccount();
        account.deposit(new BigDecimal("50.00"));
        assertEquals(new BigDecimal("150.00"), account.getBalance());
    }

    @Test
    @DisplayName("Снятие со счёта")
    void testWithdraw() {
        DebitAccount account = createTestAccount();
        account.withdraw(new BigDecimal("30.00"));
        assertEquals(new BigDecimal("70.00"), account.getBalance());
    }

    @Test
    @DisplayName("Нельзя снять больше баланса")
    void testWithdrawInsufficientBalance() {
        DebitAccount account = createTestAccount();
        assertThrows(IllegalStateException.class, () -> account.withdraw(new BigDecimal("200.00")));
    }

    @Test
    @DisplayName("Закрытие счёта с нулевым балансом")
    void testCloseAccount() {
        DebitAccount account = createTestAccount();
        account.withdraw(new BigDecimal("100.00"));
        account.close();
        assertEquals(AccountStatus.CLOSED, account.getStatus());
    }

    @Test
    @DisplayName("Нельзя закрыть счёт с деньгами")
    void testCloseAccountWithBalance() {
        DebitAccount account = createTestAccount();
        assertThrows(IllegalStateException.class, () -> account.close());
    }
}