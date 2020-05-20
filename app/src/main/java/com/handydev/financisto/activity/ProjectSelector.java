package com.handydev.financisto.activity;

import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.List;

import com.handydev.financisto.R;
import com.handydev.financisto.db.DatabaseAdapter;
import com.handydev.financisto.db.MyEntityManager;
import com.handydev.financisto.model.Project;
import com.handydev.financisto.utils.MyPreferences;
import com.handydev.financisto.utils.TransactionUtils;

/**
 * Created by IntelliJ IDEA.
 * User: denis.solonenko
 * Date: 7/2/12 9:25 PM
 */
public class ProjectSelector<A extends AbstractActivity> extends MyEntitySelector<Project, A> {

    public ProjectSelector(A activity, DatabaseAdapter db, ActivityLayout x) {
        this(activity, db, x, R.string.no_project);
    }

    public ProjectSelector(A activity, DatabaseAdapter db, ActivityLayout x, int emptyId) {
        super(Project.class, activity, db, x, MyPreferences.isShowProject(activity),
                R.id.project, R.id.project_add, R.id.project_clear, R.string.project, emptyId,
                R.id.project_show_list, R.id.project_close_filter, R.id.project_show_filter, MyEntitySelector.REQUEST_PROJECT);
    }

    @Override
    protected Class getEditActivityClass() {
        return ProjectActivity.class;
    }

    @Override
    protected List<Project> fetchEntities(MyEntityManager em) {
        return em.getActiveProjectsList(true);
    }

    @Override
    protected ListAdapter createAdapter(Activity activity, List<Project> entities) {
        return TransactionUtils.createProjectAdapter(activity, entities);
    }

    @Override
    protected ArrayAdapter<Project> createFilterAdapter() {
        return TransactionUtils.projectFilterAdapter(activity, em);
    }

    @Override
    protected boolean isListPickConfigured() {
        return MyPreferences.isProjectSelectorList(activity);
    }

}
