package com.handydev.financisto.activity;

import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.List;

import com.handydev.financisto.R;
import com.handydev.financisto.db.MyEntityManager;
import com.handydev.financisto.model.Payee;
import com.handydev.financisto.utils.MyPreferences;
import com.handydev.financisto.utils.TransactionUtils;

public class PayeeSelector<A extends AbstractActivity> extends MyEntitySelector<Payee,A> {

    public PayeeSelector(A activity, MyEntityManager em, ActivityLayout x) {
        this(activity, em, x, R.string.no_payee);
    }

    public PayeeSelector(A activity, MyEntityManager em, ActivityLayout x, int emptyId) {
        super(Payee.class, activity, em, x, MyPreferences.isShowPayee(activity),
                R.id.payee, R.id.payee_add, R.id.payee_clear, R.string.payee, emptyId,
                R.id.payee_show_list, R.id.payee_close_filter, R.id.payee_show_filter, MyEntitySelector.REQUEST_PAYEE);
    }

    @Override
    protected Class getEditActivityClass() {
        return PayeeActivity.class;
    }

    @Override
    protected List<Payee> fetchEntities(MyEntityManager em) {
        return em.getAllActivePayeeList();
    }

    @Override
    protected ListAdapter createAdapter(Activity activity, List<Payee> entities) {
        return TransactionUtils.createPayeeAdapter(activity, entities);
    }

    @Override
    protected ArrayAdapter<Payee> createFilterAdapter() {
        return TransactionUtils.payeeFilterAdapter(activity, em);
    }

    @Override
    protected boolean isListPickConfigured() {
        return MyPreferences.isPayeeSelectorList(activity);
    }

}
