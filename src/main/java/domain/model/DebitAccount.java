package domain.model;

import java.math.BigDecimal;

public class DebitAccount extends Account {
    public DebitAccount(
            String accountId,
            String number,
            BigDecimal balance,
            String currency,
            AccountStatus status,
            Customer owner) {
        super(accountId, number, balance, currency, status, owner);
    }
}

