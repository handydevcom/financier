package com.handydev.financier.activity;

import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import com.handydev.financier.R;
import com.handydev.financier.blotter.BlotterFilter;
import com.handydev.financier.filter.Criteria;
import com.handydev.financier.filter.WhereFilter;
import com.handydev.financier.model.Category;
import com.handydev.financier.model.MultiChoiceItem;
import com.handydev.financier.model.MyEntity;
import com.handydev.financier.utils.ArrUtils;

import static com.handydev.financier.activity.CategorySelector.SelectorType.FILTER;
import static com.handydev.financier.blotter.BlotterFilter.CATEGORY_LEFT;
import static com.handydev.financier.blotter.BlotterFilter.LOCATION_ID;
import static com.handydev.financier.blotter.BlotterFilter.PAYEE_ID;
import static com.handydev.financier.blotter.BlotterFilter.PROJECT_ID;
import static com.handydev.financier.filter.WhereFilter.Operation.BTW;
import static com.handydev.financier.filter.WhereFilter.Operation.IN;

public abstract class FilterAbstractActivity extends AbstractActivity implements CategorySelector.CategorySelectorListener {

    protected WhereFilter filter = WhereFilter.empty();

    protected ProjectSelector<FilterAbstractActivity> projectSelector;
    protected PayeeSelector<FilterAbstractActivity> payeeSelector;
    protected CategorySelector<FilterAbstractActivity> categorySelector;
    protected LocationSelector<FilterAbstractActivity> locationSelector;

    protected String noFilterValue;

    protected void initPayeeSelector(LinearLayout layout) {
        initPayeeSelector(layout, false);
    }

    protected void initPayeeSelector(LinearLayout layout, Boolean darkUI) {
        payeeSelector = new PayeeSelector<>(this, db, activityLayout, R.string.no_filter);
        payeeSelector.initMultiSelect();
        payeeSelector.createNode(layout, darkUI);
    }

    protected void initProjectSelector(LinearLayout layout) {
        initProjectSelector(layout, false);
    }

    protected void initProjectSelector(LinearLayout layout, Boolean darkUI) {
        projectSelector = new ProjectSelector<>(this, db, activityLayout, R.string.no_filter);
        projectSelector.initMultiSelect();
        projectSelector.createNode(layout, darkUI);
    }

    protected void initLocationSelector(LinearLayout layout) {
        initLocationSelector(layout, false);
    }

    protected void initLocationSelector(LinearLayout layout, Boolean darkUI) {
        locationSelector = new LocationSelector<>(this, db, activityLayout, R.string.no_filter);
        locationSelector.initMultiSelect();
        locationSelector.createNode(layout, darkUI);
    }

    protected void initCategorySelector(LinearLayout layout) {
        initCategorySelector(layout, false);
    }

    protected void initCategorySelector(LinearLayout layout, Boolean darkUI) {
        categorySelector = new CategorySelector<>(this, db, activityLayout, -1, true);
        categorySelector.setListener(this);
        categorySelector.initMultiSelect();
        categorySelector.createNode(layout, FILTER, darkUI);
    }

    protected void clear(String criteria) {
        filter.remove(criteria);
    }

    protected void clear(String criteria, TextView textView) {
        filter.remove(criteria);
        textView.setText(R.string.no_filter);
        hideMinusButton(textView);
    }

    protected void clearCategoryFilter() {
        clear(CATEGORY_LEFT);
    }

    @Override
    protected void onClick(View v, int id) {
        switch (id) {
            case R.id.category:
            case R.id.category_show_list:
            case R.id.category_close_filter:
            case R.id.category_show_filter:
                categorySelector.onClick(id);
            break;
            case R.id.category_clear:
                categorySelector.onClick(id);
                clearCategoryFilter();
                break;
            case R.id.project: {
                Criteria c = filter.get(PROJECT_ID);
                if (c != null) projectSelector.updateCheckedEntities(c.getValues());
                projectSelector.onClick(id);
            }
            break;
            case R.id.project_clear:
                clear(PROJECT_ID);
                projectSelector.onClick(id);
                break;
            case R.id.project_show_filter:
            case R.id.project_close_filter:
            case R.id.project_show_list:
                projectSelector.onClick(id);
                break;
            case R.id.location: {
                Criteria c = filter.get(LOCATION_ID);
                if (c != null) locationSelector.updateCheckedEntities(c.getValues());
                locationSelector.onClick(id);
            }
            break;
            case R.id.location_clear:
                clear(LOCATION_ID);
                locationSelector.onClick(id);
                break;
            case R.id.location_show_filter:
            case R.id.location_close_filter:
            case R.id.location_show_list:
                locationSelector.onClick(id);
                break;
            case R.id.payee: {
                Criteria c = filter.get(BlotterFilter.PAYEE_ID);
                if (c != null) projectSelector.updateCheckedEntities(c.getValues());
                payeeSelector.onClick(id);
            }
            break;
            case R.id.payee_clear:
                clear(BlotterFilter.PAYEE_ID);
                payeeSelector.onClick(id);
                break;
            case R.id.payee_show_filter:
            case R.id.payee_close_filter:
            case R.id.payee_show_list:
                payeeSelector.onClick(id);
                break;
        }
    }

    @Override
    public void onSelectedId(final int id, final long selectedId) {
        switch (id) {
            case R.id.project:
                projectSelector.onSelectedId(id, selectedId);
                filter.put(Criteria.in(PROJECT_ID, projectSelector.getCheckedIds()));
                updateProjectFromFilter();
                break;
            case R.id.payee:
                payeeSelector.onSelectedId(id, selectedId);
                if (selectedId == 0) {
                    filter.put(Criteria.isNull(BlotterFilter.PAYEE_ID));
                } else {
                    filter.put(Criteria.in(BlotterFilter.PAYEE_ID, payeeSelector.getCheckedIds()));
                }
                updatePayeeFromFilter();
                break;
            case R.id.category:
                categorySelector.onSelectedId(id, selectedId, false);
//                filter.put(Criteria.btw(CATEGORY_LEFT, categorySelector.getCheckedCategoryLeafs()));
//                updateCategoryFromFilter();
                break;
            case R.id.location:
                locationSelector.onSelectedId(id, selectedId);
                filter.put(Criteria.in(LOCATION_ID, locationSelector.getCheckedIds()));
                updateLocationFromFilter();
                break;
        }
    }

    @Override
    public void onSelected(int id, List<? extends MultiChoiceItem> items) {
        switch (id) {
            case R.id.category:
                if (ArrUtils.isEmpty(categorySelector.getCheckedCategoryLeafs())) {
                    clearCategoryFilter();
                } else {
                    filter.put(Criteria.btw(CATEGORY_LEFT, categorySelector.getCheckedCategoryLeafs()));
                    updateCategoryFromFilter();
                }
                break;
            case R.id.project:
                if (ArrUtils.isEmpty(projectSelector.getCheckedIds())) {
                    clear(PROJECT_ID);
                } else {
                    filter.put(Criteria.in(PROJECT_ID, projectSelector.getCheckedIds()));
                    updateProjectFromFilter();
                }
                break;
            case R.id.payee:
                if (ArrUtils.isEmpty(payeeSelector.getCheckedIds())) {
                    clear(PAYEE_ID);
                } else {
                    filter.put(Criteria.in(PAYEE_ID, payeeSelector.getCheckedIds()));
                    updatePayeeFromFilter();
                }
                break;
            case R.id.location:
                if (ArrUtils.isEmpty(locationSelector.getCheckedIds())) {
                    clear(LOCATION_ID);
                } else {
                    filter.put(Criteria.in(LOCATION_ID, locationSelector.getCheckedIds()));
                    updateLocationFromFilter();
                }
                break;
        }
    }

    @Override
    public void onSelectedPos(int id, int selectedPos) { // todo.mb: not used in case of multi-select, so remove then
        switch (id) {
            case R.id.project:
                projectSelector.onSelectedPos(id, selectedPos);
                filter.put(Criteria.eq(PROJECT_ID, String.valueOf(projectSelector.getSelectedEntityId())));
                updateProjectFromFilter();
                break;
            case R.id.payee:
                payeeSelector.onSelectedPos(id, selectedPos);
                filter.put(Criteria.eq(PAYEE_ID, String.valueOf(payeeSelector.getSelectedEntityId())));
                updatePayeeFromFilter();
                break;
        }
    }

    protected void updateCategoryFromFilter() {
        Criteria c = filter.get(CATEGORY_LEFT);
        if (c != null) {
            if (c.operation != BTW) { // todo.mb: only for backward compatibility, just remove in next releases
                Log.i("Financier", "Found category filter with deprecated op: " + c.operation);
                filter.remove(CATEGORY_LEFT);
                return;
            }

            List<String> checkedLeftIds = getLeftCategoryNodesFromFilter(c);
            List<Long> catIds = db.getCategoryIdsByLeftIds(checkedLeftIds);
            categorySelector.updateCheckedEntities(catIds);
            categorySelector.fillCategoryInUI();
        }
    }

    private List<String> getLeftCategoryNodesFromFilter(Criteria catCriteria) {
        List<String> res = new LinkedList<>();
        for (int i = 0; i < catCriteria.getValues().length; i += 2) {
            res.add(catCriteria.getValues()[i]);
        }
        return res;
    }

    protected <T extends MyEntity> void updateEntityFromFilter(String filterCriteriaName, MyEntitySelector<T, FilterAbstractActivity> entitySelector) {
        Criteria c = filter.get(filterCriteriaName);
        if (c != null && !c.isNull()) {
            if (c.operation == IN) {
                entitySelector.updateCheckedEntities(c.getValues());
                entitySelector.fillCheckedEntitiesInUI();
            } else {
                long entityId = c.getLongValue1();
                entitySelector.selectEntity(entityId);
            }
        } else {
            entitySelector.selectEntity(null);
        }
    }

    protected <T extends MyEntity> void updateEntityFromFilter(String filterCriteriaName, Class<T> entityClass, TextView filterView) {
        Criteria c = filter.get(filterCriteriaName);
        if (c != null && !c.isNull()) {
            String filterText = noFilterValue;
            long entityId = c.getLongValue1();
            T e = db.get(entityClass, entityId);
            if (e != null) filterText = e.title;
            if (!TextUtils.isEmpty(filterText)) {
                filterView.setText(filterText);
                showMinusButton(filterView);
            }
        } else {
            filterView.setText(R.string.no_filter);
            hideMinusButton(filterView);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CategorySelectorActivity.CATEGORY_PICK:
            case CategorySelectorActivity.CATEGORY_ADD:
                categorySelector.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    public void onCategorySelected(Category cat, boolean selectLast) {
        clearCategoryFilter();
        if (categorySelector.isMultiSelect()) {
            final String[] checkedCatLeafs = categorySelector.getCheckedCategoryLeafs();
            if (checkedCatLeafs.length > 1)
                filter.put(Criteria.btw(CATEGORY_LEFT, checkedCatLeafs));
        } else {
            if (cat.id > 0) {
                filter.put(Criteria.btw(CATEGORY_LEFT, String.valueOf(cat.left), String.valueOf(cat.right)));
            }
        }
        updateCategoryFromFilter();
    }

    protected void updateProjectFromFilter() {
        if (projectSelector.isShow()) {
            updateEntityFromFilter(PROJECT_ID, projectSelector);
        }
    }

    protected void updatePayeeFromFilter() {
        if (payeeSelector.isShow()) {
            updateEntityFromFilter(BlotterFilter.PAYEE_ID, payeeSelector);
        }
    }

    protected void updateLocationFromFilter() {
        if (locationSelector.isShow()) {
            updateEntityFromFilter(LOCATION_ID, locationSelector);
        }
    }

    protected void showMinusButton(TextView textView) {
        ImageView v = findMinusButton(textView);
        v.setVisibility(View.VISIBLE);
    }

    protected void hideMinusButton(TextView textView) {
        ImageView v = findMinusButton(textView);
        v.setVisibility(View.GONE);
    }

    protected ImageView findMinusButton(TextView textView) {
        return (ImageView) textView.getTag(R.id.bMinus);
    }

    @Override
    protected void onDestroy() {
        if (categorySelector != null) categorySelector.onDestroy();
        super.onDestroy();
    }
}
