package com.handydev.financisto.model.rates;

import com.handydev.financisto.db.AbstractDbTest;
import com.handydev.financisto.rates.ExchangeRate;
import com.handydev.financisto.test.DateTime;

import static org.junit.Assert.*;

public abstract class AssertExchangeRate extends AbstractDbTest {

    public static void assertRate(DateTime date, double rate, ExchangeRate r) {
        assertEquals(rate, r.rate, 0.00001d);
        assertEquals(date.atMidnight().asLong(), r.date);
    }

}
