package com.handydev.financier.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.Color
import android.text.format.DateUtils
import com.handydev.financier.utils.TransactionTitleUtils.generateTransactionTitle
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.adapter.BlotterListAdapter
import com.handydev.financier.adapter.BlotterListAdapter.BlotterViewHolder
import com.handydev.financier.db.DatabaseHelper.BlotterColumns
import com.handydev.financier.R
import com.handydev.financier.utils.CurrencyCache
import com.handydev.financier.utils.StringUtil
import com.handydev.financier.utils.Utils

class TransactionsListAdapter(context: Context?, db: DatabaseAdapter?, c: Cursor?) :
    BlotterListAdapter(context, db, c) {
    override fun bindView(v: BlotterViewHolder, context: Context, cursor: Cursor) {
        val toAccountId = cursor.getLong(BlotterColumns.to_account_id.ordinal)
        val payee = cursor.getString(BlotterColumns.payee.ordinal)
        var note = cursor.getString(BlotterColumns.note.ordinal)
        val locationId = cursor.getLong(BlotterColumns.location_id.ordinal)
        var location: String? = ""
        if (locationId > 0) {
            location = cursor.getString(BlotterColumns.location.ordinal)
        }
        val toAccount = cursor.getString(BlotterColumns.to_account_title.ordinal)
        val fromAmount = cursor.getLong(BlotterColumns.from_amount.ordinal)
        if (toAccountId > 0) {
            v.topView.setText(R.string.transfer)
            note = if (fromAmount > 0) {
                "$toAccount \u00BB"
            } else {
                "\u00AB $toAccount"
            }
        } else {
            val title = cursor.getString(BlotterColumns.from_account_title.ordinal)
            v.topView.text = title
            v.centerView.setTextColor(Color.WHITE)
        }
        val categoryId = cursor.getLong(BlotterColumns.category_id.ordinal)
        var category: String? = ""
        if (categoryId != 0L) {
            category = cursor.getString(BlotterColumns.category_title.ordinal)
        }
        val text = generateTransactionTitle(sb, payee, note, location, categoryId, category!!)
        v.centerView.text = text
        sb.setLength(0)
        val currencyId = cursor.getLong(BlotterColumns.from_account_currency_id.ordinal)
        val c = CurrencyCache.getCurrency(db, currencyId)
        val originalCurrencyId = cursor.getLong(BlotterColumns.original_currency_id.ordinal)
        if (originalCurrencyId > 0) {
            val originalCurrency = CurrencyCache.getCurrency(db, originalCurrencyId)
            val originalAmount = cursor.getLong(BlotterColumns.original_from_amount.ordinal)
            u.setAmountText(
                sb,
                v.rightCenterView,
                originalCurrency,
                originalAmount,
                c,
                fromAmount,
                true
            )
        } else {
            u.setAmountText(v.rightCenterView, c, fromAmount, true)
        }
        if (fromAmount > 0) {
            v.iconView.setImageDrawable(icBlotterIncome)
            v.iconView.setColorFilter(u.positiveColor)
        } else if (fromAmount < 0) {
            v.iconView.setImageDrawable(icBlotterExpense)
            v.iconView.setColorFilter(u.negativeColor)
        }
        val date = cursor.getLong(BlotterColumns.datetime.ordinal)
        v.bottomView.text = StringUtil.capitalize(
            DateUtils.formatDateTime(
                context, date,
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY
            )
        )
        if (date > System.currentTimeMillis()) {
            u.setFutureTextColor(v.bottomView)
        } else {
            v.bottomView.setTextColor(v.topView.textColors.defaultColor)
        }
        val balance = cursor.getLong(BlotterColumns.from_account_balance.ordinal)
        v.rightView.text = Utils.amountToString(c, balance, false)
        removeRightViewIfNeeded(v)
        setIndicatorColor(v, cursor)
        alternateColorIfNeeded(v, context, cursor)
    }
}