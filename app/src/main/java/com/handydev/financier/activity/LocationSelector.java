package com.handydev.financier.activity;

import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.List;

import com.handydev.financier.R;
import com.handydev.financier.db.DatabaseAdapter;
import com.handydev.financier.db.MyEntityManager;
import com.handydev.financier.model.MyLocation;
import com.handydev.financier.utils.MyPreferences;
import com.handydev.financier.utils.TransactionUtils;

import org.jetbrains.annotations.Nullable;

public class LocationSelector<A extends AbstractActivity> extends MyEntitySelector<MyLocation, A> {

    public LocationSelector(A activity, DatabaseAdapter db, ActivityLayout x) {
        this(activity, db, x, R.string.current_location);
    }

    public LocationSelector(A activity, DatabaseAdapter db, ActivityLayout x, int emptyId) {
        super(MyLocation.class, activity, db, x, MyPreferences.isShowLocation(activity),
                R.id.location, R.id.location_add, R.id.location_clear, R.string.location, emptyId,
                R.id.location_show_list, R.id.location_close_filter, R.id.location_show_filter, MyEntitySelector.REQUEST_LOCATION);
    }

    @Override
    protected Class getEditActivityClass() {
        return LocationActivity.class;
    }

    @Override
    protected List<MyLocation> fetchEntities(MyEntityManager em) {
        return em.getActiveLocationsList(true);
    }

    @Override
    protected ArrayAdapter<MyLocation> createFilterAdapter() {
        return TransactionUtils.locationFilterAdapter(getActivity(), getEm());
    }

    @Override
    protected boolean isListPickConfigured() {
        return MyPreferences.isLocationSelectorList(getActivity());
    }

    @Nullable
    @Override
    protected ListAdapter createAdapter(@Nullable Activity activity, @Nullable List<? extends MyLocation> entities) {
        return TransactionUtils.createLocationAdapter(activity, entities);
    }
}
