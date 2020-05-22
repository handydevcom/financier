/*
 * Copyright (c) 2012 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package com.handydev.financier.blotter;

import android.content.Context;
import android.widget.TextView;
import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.db.TransactionsTotalCalculator;
import com.handydev.financier.filter.WhereFilter;
import com.handydev.financier.model.Total;

import static com.handydev.financier.db.DatabaseAdapter.enhanceFilterForAccountBlotter;

public class AccountTotalCalculationTask extends TotalCalculationTask {

	private final DatabaseAdapter db;
	private final WhereFilter filter;

	public AccountTotalCalculationTask(Context context, DatabaseAdapter db, WhereFilter filter, TextView totalText) {
        super(context, totalText);
		this.db = db;
		this.filter = enhanceFilterForAccountBlotter(filter);
	}

    @Override
    public Total getTotalInHomeCurrency() {
        TransactionsTotalCalculator calculator = new TransactionsTotalCalculator(db, filter);
        return calculator.getAccountTotal();
    }

    @Override
    public Total[] getTotals() {
        TransactionsTotalCalculator calculator = new TransactionsTotalCalculator(db, filter);
        return calculator.getTransactionsBalance();
    }

}
