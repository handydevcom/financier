package com.handydev.financier.activity;

import android.app.Activity;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import java.util.List;

import com.handydev.financier.R;
import com.handydev.financier.db.MyEntityManager;
import com.handydev.financier.model.Payee;
import com.handydev.financier.utils.MyPreferences;
import com.handydev.financier.utils.TransactionUtils;

import org.jetbrains.annotations.Nullable;

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
    protected ArrayAdapter<Payee> createFilterAdapter() {
        return TransactionUtils.payeeFilterAdapter(getActivity(), getEm());
    }

    @Override
    protected boolean isListPickConfigured() {
        return MyPreferences.isPayeeSelectorList(getActivity());
    }

    @Nullable
    @Override
    protected ListAdapter createAdapter(@Nullable Activity activity, @Nullable List<? extends Payee> entities) {
       return TransactionUtils.createPayeeAdapter(activity, entities);
    }
}
