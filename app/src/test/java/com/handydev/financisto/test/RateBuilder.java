/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.handydev.financisto.test;

import com.handydev.financisto.db.DatabaseAdapter;
import com.handydev.financisto.model.Currency;
import com.handydev.financisto.rates.ExchangeRate;

/**
 * Created by IntelliJ IDEA.
 * User: Denis Solonenko
 * Date: 3/2/11 9:11 PM
 */
public class RateBuilder {

    private final DatabaseAdapter db;
    private final ExchangeRate r = new ExchangeRate();

    public static RateBuilder inMemory() {
        return new RateBuilder(null);
    }

    public static RateBuilder withDb(DatabaseAdapter db) {
        return new RateBuilder(db);
    }

    private RateBuilder(DatabaseAdapter db) {
        this.db = db;
    }

    public RateBuilder from(Currency c) {
        r.fromCurrencyId = c.id;
        return this;
    }

    public RateBuilder to(Currency c) {
        r.toCurrencyId = c.id;
        return this;
    }


    public RateBuilder at(DateTime date) {
        r.date = date.asLong();
        return this;
    }

    public RateBuilder rate(float rate) {
        r.rate = rate;
        return this;
    }

    public RateBuilder notOK() {
        r.error = "Exception";
        return this;
    }

    public ExchangeRate create() {
        if (db != null) {
            db.saveRate(r);
        }
        return r;
    }

}
