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
package com.handydev.financier.activity;

import android.view.View;
import com.handydev.financier.R;
import com.handydev.financier.blotter.BlotterFilter;
import com.handydev.financier.filter.Criteria;
import com.handydev.financier.model.Project;

public class ProjectListActivity extends MyEntityListActivity<Project> {

    public ProjectListActivity() {
        super(Project.class, R.string.no_projects);
    }

    @Override
    protected Class<ProjectActivity> getEditActivityClass() {
        return ProjectActivity.class;
    }

    @Override
    protected Criteria createBlotterCriteria(Project p) {
        return Criteria.eq(BlotterFilter.PROJECT_ID, String.valueOf(p.id));
    }

    @Override
    protected void deleteItem(View v, int position, long id) {
        db.deleteProject(id);
        recreateCursor();
    }

}
