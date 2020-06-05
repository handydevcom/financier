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
package com.handydev.financier.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;

import com.handydev.financier.R;
import com.handydev.financier.datetime.DateUtils;
import com.handydev.financier.model.Account;
import com.handydev.financier.model.AccountType;
import com.handydev.financier.model.CardIssuer;
import com.handydev.financier.model.ElectronicPaymentType;
import com.handydev.financier.utils.MyPreferences;
import com.handydev.financier.utils.Utils;
import com.handydev.orb.EntityManager;

import java.text.DateFormat;
import java.util.Date;

public class AccountListAdapter2 extends ResourceCursorAdapter {

    private final Utils u;
    private DateFormat df;
    private boolean isShowAccountLastTransactionDate;

    public AccountListAdapter2(Context context, Cursor c) {
        super(context, R.layout.account_list_item, c);
        this.u = new Utils(context);
        this.df = DateUtils.getShortDateFormat(context);
        this.isShowAccountLastTransactionDate = MyPreferences.isShowAccountLastTransactionDate(context);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View view = super.newView(context, cursor, parent);
        return AccountListItemHolder.create(view);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        Account a = EntityManager.loadFromCursor(cursor, Account.class);
        AccountListItemHolder v = (AccountListItemHolder) view.getTag();

        v.accountNameView.setText(a.title);

        AccountType type = AccountType.valueOf(a.type);
        if (type.isCard && a.cardIssuer != null) {
            CardIssuer cardIssuer = CardIssuer.valueOf(a.cardIssuer);
            v.iconView.setImageResource(cardIssuer.iconId);
        } else if (type.isElectronic && a.cardIssuer != null) {
            ElectronicPaymentType paymentType = ElectronicPaymentType.valueOf(a.cardIssuer);
            v.iconView.setImageResource(paymentType.iconId);
        } else {
            v.iconView.setImageResource(type.iconId);
        }
        if (a.isActive) {
            v.iconView.getDrawable().mutate().setAlpha(0xFF);
            v.lockIconView.setVisibility(View.INVISIBLE);
        } else {
            v.iconView.getDrawable().mutate().setAlpha(0x77);
            v.lockIconView.setVisibility(View.VISIBLE);
        }

        StringBuilder sb = new StringBuilder();
        if (!Utils.isEmpty(a.issuer)) {
            sb.append(a.issuer);
        }
        if (!Utils.isEmpty(a.number)) {
            sb.append(" #").append(a.number);
        }
        if (sb.length() == 0) {
            sb.append(context.getString(type.titleId));
        }
        v.accountDescriptionView.setText(sb.toString());

        long date = a.creationDate;
        if (isShowAccountLastTransactionDate && a.lastTransactionDate > 0) {
            date = a.lastTransactionDate;
        }
        v.lastTransactionDate.setText(df.format(new Date(date)));

        long amount = a.totalAmount;
        if (type == AccountType.CREDIT_CARD && a.limitAmount != 0) {
            long limitAmount = Math.abs(a.limitAmount);
            long balance = limitAmount + amount;
            long balancePercentage = 10000 * balance / limitAmount;
            u.setAmountText(v.ccOwnFundsView, a.currency, amount, false);
            u.setAmountText(v.accountBalance, a.currency, balance, false);
            v.ccOwnFundsView.setVisibility(View.VISIBLE);
            v.ccProgressBar.setMax(10000);
            v.ccProgressBar.setProgress((int) balancePercentage);
            v.ccProgressBar.setVisibility(View.VISIBLE);
        } else {
            u.setAmountText(v.accountBalance, a.currency, amount, false);
            v.ccOwnFundsView.setVisibility(View.GONE);
            v.ccProgressBar.setVisibility(View.GONE);
        }
        alternateColorIfNeeded(v, context, cursor);
    }

    protected void alternateColorIfNeeded(AccountListItemHolder v, Context context, Cursor cursor) {
        if(MyPreferences.isAccountAlternateColors(context)) {
            if(cursor.getPosition() % 2 == 1) {
                v.layout.setBackgroundColor(Color.argb(255, 31, 31, 31));
            } else {
                v.layout.setBackgroundColor(Color.TRANSPARENT);
            }
        } else {
            v.layout.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private static class AccountListItemHolder {
        ImageView iconView;
        ImageView lockIconView;
        TextView accountDescriptionView;
        TextView accountNameView;
        TextView lastTransactionDate;
        TextView accountBalance;
        TextView ccOwnFundsView;
        ProgressBar ccProgressBar;
        ConstraintLayout layout;

        public static View create(View view) {
            AccountListItemHolder v = new AccountListItemHolder();
            v.iconView = view.findViewById(R.id.icon);
            v.lockIconView = view.findViewById(R.id.lock_icon);
            v.accountDescriptionView = view.findViewById(R.id.account_description);
            v.accountNameView = view.findViewById(R.id.account_name);
            v.lastTransactionDate = view.findViewById(R.id.last_transaction_date);
            v.accountBalance = view.findViewById(R.id.balance);
            v.ccOwnFundsView = view.findViewById(R.id.cc_own_funds);
            v.ccOwnFundsView.setVisibility(View.GONE);
            v.ccProgressBar = view.findViewById(R.id.cc_progress);
            v.ccProgressBar.setVisibility(View.GONE);
            v.layout = view.findViewById(R.id.account_list_item_layout);
            view.setTag(v);
            return view;
        }

    }


}
