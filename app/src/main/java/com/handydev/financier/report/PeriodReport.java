/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 * 
 * Contributors:
 *     Denis Solonenko - initial API and implementation
 ******************************************************************************/
package com.handydev.financier.report;

import android.content.Context;
import android.database.Cursor;
import com.handydev.financier.filter.WhereFilter;
import com.handydev.financier.datetime.Period;
import com.handydev.financier.datetime.PeriodType;
import com.handydev.financier.filter.Criteria;
import com.handydev.financier.filter.DateTimeCriteria;
import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.db.DatabaseHelper.ReportColumns;
import com.handydev.financier.graph.GraphUnit;
import com.handydev.financier.model.Currency;
import com.handydev.financier.model.Total;

import java.util.ArrayList;

import static com.handydev.financier.db.DatabaseHelper.V_REPORT_PERIOD;
import static com.handydev.financier.datetime.PeriodType.*;

public class PeriodReport extends Report {

    private final PeriodType[] periodTypes = new PeriodType[]{TODAY, YESTERDAY, THIS_WEEK, LAST_WEEK, THIS_AND_LAST_WEEK, THIS_MONTH, LAST_MONTH, THIS_AND_LAST_MONTH};
	private final Period[] periods = new Period[periodTypes.length];

    private Period currentPeriod;

	public PeriodReport(Context context, Currency currency) {
		super(ReportType.BY_PERIOD, context, currency);
        for (int i=0; i<periodTypes.length; i++) {
            periods[i] = periodTypes[i].calculatePeriod();
        }
    }

	@Override
	public ReportData getReport(DatabaseAdapter db, WhereFilter filter) {
		WhereFilter newFilter = WhereFilter.empty();
		Criteria criteria = filter.get(ReportColumns.FROM_ACCOUNT_CURRENCY_ID);
		if (criteria != null) {
			newFilter.put(criteria);
		}
		filterTransfers(newFilter);
		ArrayList<GraphUnit> units = new ArrayList<GraphUnit>();
        for (Period p : periods) {
            currentPeriod = p;
            newFilter.put(Criteria.btw(ReportColumns.DATETIME, String.valueOf(p.start), String.valueOf(p.end)));
            Cursor c = db.db().query(V_REPORT_PERIOD, ReportColumns.NORMAL_PROJECTION,
                    newFilter.getSelection(), newFilter.getSelectionArgs(), null, null, null);
            ArrayList<GraphUnit> u = getUnitsFromCursor(db, c);
            if (u.size() > 0 && u.get(0).size() > 0) {
                units.add(u.get(0));
            }
        }
        Total total = calculateTotal(units);
		return new ReportData(units, total);
	}

    @Override
    protected long getId(Cursor c) {
        return currentPeriod.type.ordinal();
    }

    @Override
    protected String alterName(long id, String name) {
        return context.getString(currentPeriod.type.titleId);
    }

    @Override
	public Criteria getCriteriaForId(DatabaseAdapter db, long id) {
        for (Period period : periods) {
            if (period.type.ordinal() == id) {
                return new DateTimeCriteria(period);
            }
        }
		return null;
	}

    @Override
    public boolean shouldDisplayTotal() {
        return false;
    }

}
