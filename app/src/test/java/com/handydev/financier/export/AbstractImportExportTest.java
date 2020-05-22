package com.handydev.financier.export;

import com.handydev.financier.db.AbstractDbTest;
import com.handydev.financier.model.Account;
import com.handydev.financier.model.AccountType;
import com.handydev.financier.model.Currency;
import com.handydev.financier.test.CurrencyBuilder;

import static org.junit.Assert.assertNotNull;

public abstract class AbstractImportExportTest extends AbstractDbTest {

    protected Account createFirstAccount() {
        Currency c = createCurrency("SGD");
        Account a = new Account();
        a.title = "My Cash Account";
        a.type = AccountType.CASH.name();
        a.currency = c;
        a.totalAmount = 0;
        a.sortOrder = 100;
        a.note = "AAA\nBBB:CCC";
        db.saveAccount(a);
        assertNotNull(db.load(Account.class, a.id));
        return a;
    }

    protected Account createSecondAccount() {
        Currency c = createCurrency("CZK");
        Account a = new Account();
        a.title = "My Bank Account";
        a.type = AccountType.BANK.name();
        a.currency = c;
        a.totalAmount = 0;
        a.sortOrder = 50;
        db.saveAccount(a);
        assertNotNull(db.load(Account.class, a.id));
        return a;
    }

    private Currency createCurrency(String currency) {
        Currency c = CurrencyBuilder.withDb(db)
                .title("Singapore Dollar")
                .name(currency)
                .separators("''", "'.'")
                .symbol("S$")
                .create();
        assertNotNull(db.load(Currency.class, c.id));
        return c;
    }

}
