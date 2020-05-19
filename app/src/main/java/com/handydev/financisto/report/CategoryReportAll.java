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
package com.handydev.financisto.report;

import android.content.Context;
import com.handydev.financisto.activity.BlotterActivity;
import com.handydev.financisto.activity.SplitsBlotterActivity;
import com.handydev.financisto.blotter.BlotterFilter;
import com.handydev.financisto.filter.WhereFilter;
import com.handydev.financisto.filter.Criteria;
import com.handydev.financisto.db.DatabaseAdapter;
import com.handydev.financisto.model.Category;
import com.handydev.financisto.model.Currency;

import static com.handydev.financisto.db.DatabaseHelper.V_REPORT_CATEGORY;

public class CategoryReportAll extends Report {

	public CategoryReportAll(Context context, Currency currency) {
		super(ReportType.BY_CATEGORY, context, currency);
	}

	@Override
	public ReportData getReport(DatabaseAdapter db, WhereFilter filter) {
        cleanupFilter(filter);
		return queryReport(db, V_REPORT_CATEGORY, filter);
	}
	
	@Override
	public Criteria getCriteriaForId(DatabaseAdapter db, long id) {
		Category c = db.getCategoryWithParent(id);
		return Criteria.btw(BlotterFilter.CATEGORY_LEFT, String.valueOf(c.left), String.valueOf(c.right));
	}

    @Override
    public boolean shouldDisplayTotal() {
        return false;
    }

    @Override
    protected Class<? extends BlotterActivity> getBlotterActivityClass() {
        return SplitsBlotterActivity.class;
    }

}
