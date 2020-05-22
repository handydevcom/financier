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
import com.handydev.financier.activity.BlotterActivity;
import com.handydev.financier.activity.SplitsBlotterActivity;
import com.handydev.financier.blotter.BlotterFilter;
import com.handydev.financier.filter.WhereFilter;
import com.handydev.financier.filter.Criteria;
import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.model.Currency;

import static com.handydev.financier.db.DatabaseHelper.V_REPORT_PROJECTS;

public class ProjectsReport extends Report {

	public ProjectsReport(Context context, Currency currency) {
		super(ReportType.BY_PROJECT, context, currency);
	}

	@Override
	public ReportData getReport(DatabaseAdapter db, WhereFilter filter) {
        cleanupFilter(filter);
		return queryReport(db, V_REPORT_PROJECTS, filter);
	}

	@Override
	public Criteria getCriteriaForId(DatabaseAdapter db, long id) {
		return Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(id));
	}

    @Override
    protected Class<? extends BlotterActivity> getBlotterActivityClass() {
        return SplitsBlotterActivity.class;
    }

}
