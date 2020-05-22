package com.handydev.financier.model.rates;

import com.handydev.financier.db.AbstractDbTest;
import com.handydev.financier.rates.ExchangeRate;
import com.handydev.financier.test.DateTime;

import static org.junit.Assert.*;

public abstract class AssertExchangeRate extends AbstractDbTest {

    public static void assertRate(DateTime date, double rate, ExchangeRate r) {
        assertEquals(rate, r.rate, 0.00001d);
        assertEquals(date.atMidnight().asLong(), r.date);
    }

}
