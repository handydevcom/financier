package com.handydev.financier.adapter

import android.content.Context
import android.database.Cursor
import android.graphics.drawable.Drawable
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.handydev.financier.R
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.db.DatabaseHelper.BlotterColumns
import com.handydev.financier.model.Category.Companion.isSplit
import com.handydev.financier.model.CategoryEntity
import com.handydev.financier.model.TransactionStatus
import com.handydev.financier.recur.Recurrence
import com.handydev.financier.utils.CurrencyCache
import com.handydev.financier.utils.MyPreferences
import com.handydev.financier.utils.StringUtil
import com.handydev.financier.utils.TransactionTitleUtils.generateTransactionTitle
import com.handydev.financier.utils.Utils
import java.util.*

open class BlotterListAdapter @JvmOverloads constructor(
    context: Context,
    db: DatabaseAdapter?,
    layoutId: Int,
    c: Cursor?,
    autoRequery: Boolean = false
) : ResourceCursorAdapter(context, layoutId, c, autoRequery) {
    private val dt = Date()
    protected val sb = StringBuilder()
    protected val icBlotterIncome: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_action_arrow_left_bottom)
    protected val icBlotterExpense: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_action_arrow_right_top)
    private val icBlotterTransfer: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_action_arrow_top_down)
    val icBlotterSplit: Drawable? = ContextCompat.getDrawable(context, R.drawable.ic_action_share)
    protected val u: Utils = Utils(context)
    protected val db: DatabaseAdapter?
    private val colors: IntArray
    private var allChecked = true
    private val checkedItems = HashMap<Long, Boolean?>()
    protected open val isShowRunningBalance: Boolean

    constructor(context: Context, db: DatabaseAdapter?, c: Cursor?) : this(
        context,
        db,
        R.layout.blotter_list_item,
        c,
        false
    ) {
    }

    private fun initializeColors(context: Context): IntArray {
        val r = context.resources
        val statuses = TransactionStatus.values()
        val count = statuses.size
        val colors = IntArray(count)
        for (i in 0 until count) {
            colors[i] = r.getColor(statuses[i].colorId)
        }
        return colors
    }

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val view = super.newView(context, cursor, parent)
        createHolder(view)
        return view
    }

    private fun createHolder(view: View) {
        val h = BlotterViewHolder(view)
        view.tag = h
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val v = view.tag as BlotterViewHolder
        bindView(v, context, cursor)
    }

    protected open fun bindView(v: BlotterViewHolder, context: Context, cursor: Cursor) {
        val toAccountId = cursor.getLong(BlotterColumns.to_account_id.ordinal)
        val isTemplate = cursor.getInt(BlotterColumns.is_template.ordinal)
        val noteView = if (isTemplate == 1) v.bottomView else v.centerView
        if (toAccountId > 0) {
            v.topView.setText(R.string.transfer)
            val fromAccountTitle = cursor.getString(BlotterColumns.from_account_title.ordinal)
            val toAccountTitle = cursor.getString(BlotterColumns.to_account_title.ordinal)
            u.setTransferTitleText(noteView, fromAccountTitle, toAccountTitle)
            val fromCurrencyId = cursor.getLong(BlotterColumns.from_account_currency_id.ordinal)
            val fromCurrency = CurrencyCache.getCurrency(db, fromCurrencyId)
            val toCurrencyId = cursor.getLong(BlotterColumns.to_account_currency_id.ordinal)
            val toCurrency = CurrencyCache.getCurrency(db, toCurrencyId)
            val fromAmount = cursor.getLong(BlotterColumns.from_amount.ordinal)
            val toAmount = cursor.getLong(BlotterColumns.to_amount.ordinal)
            val fromBalance = cursor.getLong(BlotterColumns.from_account_balance.ordinal)
            val toBalance = cursor.getLong(BlotterColumns.to_account_balance.ordinal)
            u.setTransferAmountText(
                v.rightCenterView,
                fromCurrency,
                fromAmount,
                toCurrency,
                toAmount
            )
            if (v.rightView != null) {
                u.setTransferBalanceText(
                    v.rightView,
                    fromCurrency,
                    fromBalance,
                    toCurrency,
                    toBalance
                )
            }
            v.iconView.setImageDrawable(icBlotterTransfer)
            v.iconView.setColorFilter(u.transferColor)
        } else {
            val fromAccountTitle = cursor.getString(BlotterColumns.from_account_title.ordinal)
            v.topView.text = fromAccountTitle
            setTransactionTitleText(cursor, noteView)
            sb.setLength(0)
            val fromCurrencyId = cursor.getLong(BlotterColumns.from_account_currency_id.ordinal)
            val fromCurrency = CurrencyCache.getCurrency(db, fromCurrencyId)
            val amount = cursor.getLong(BlotterColumns.from_amount.ordinal)
            val originalCurrencyId = cursor.getLong(BlotterColumns.original_currency_id.ordinal)
            if (originalCurrencyId > 0) {
                val originalCurrency = CurrencyCache.getCurrency(db, originalCurrencyId)
                val originalAmount = cursor.getLong(BlotterColumns.original_from_amount.ordinal)
                u.setAmountText(
                    sb,
                    v.rightCenterView,
                    originalCurrency,
                    originalAmount,
                    fromCurrency,
                    amount,
                    true
                )
            } else {
                u.setAmountText(sb, v.rightCenterView, fromCurrency, amount, true)
            }
            val categoryId = cursor.getLong(BlotterColumns.category_id.ordinal)
            if (isSplit(categoryId)) {
                v.iconView.setImageDrawable(icBlotterSplit)
                v.iconView.setColorFilter(u.splitColor)
            } else if (amount == 0L) {
                val categoryType = cursor.getInt(BlotterColumns.category_type.ordinal)
                if (categoryType == CategoryEntity.TYPE_INCOME) {
                    v.iconView.setImageDrawable(icBlotterIncome)
                    v.iconView.setColorFilter(u.positiveColor)
                } else if (categoryType == CategoryEntity.TYPE_EXPENSE) {
                    v.iconView.setImageDrawable(icBlotterExpense)
                    v.iconView.setColorFilter(u.negativeColor)
                }
            } else {
                if (amount > 0) {
                    v.iconView.setImageDrawable(icBlotterIncome)
                    v.iconView.setColorFilter(u.positiveColor)
                } else if (amount < 0) {
                    v.iconView.setImageDrawable(icBlotterExpense)
                    v.iconView.setColorFilter(u.negativeColor)
                }
            }
            if (v.rightView != null) {
                val balance = cursor.getLong(BlotterColumns.from_account_balance.ordinal)
                v.rightView.text = Utils.amountToString(fromCurrency, balance, false)
            }
        }
        setIndicatorColor(v, cursor)
        if (isTemplate == 1) {
            val templateName = cursor.getString(BlotterColumns.template_name.ordinal)
            v.centerView.text = templateName
        } else {
            val recurrence = cursor.getString(BlotterColumns.recurrence.ordinal)
            if (isTemplate == 2 && recurrence != null) {
                val r = Recurrence.parse(recurrence)
                v.bottomView.text = r.toInfoString(context)
                v.bottomView.setTextColor(v.topView.textColors.defaultColor)
            } else {
                val date = cursor.getLong(BlotterColumns.datetime.ordinal)
                dt.time = date
                v.bottomView.text = StringUtil.capitalize(
                    DateUtils.formatDateTime(
                        context, dt.time,
                        DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY
                    )
                )
                if (isTemplate == 0 && date > System.currentTimeMillis()) {
                    u.setFutureTextColor(v.bottomView)
                } else {
                    v.bottomView.setTextColor(v.topView.textColors.defaultColor)
                }
            }
        }
        removeRightViewIfNeeded(v)
        if (v.checkBox != null) {
            val parent = cursor.getLong(BlotterColumns.parent_id.ordinal)
            val id = if (parent > 0) parent else cursor.getLong(BlotterColumns._id.ordinal)
            v.checkBox.setOnClickListener(View.OnClickListener {
                updateCheckedState(
                    id,
                    allChecked xor v.checkBox.isChecked
                )
            })
            val isChecked = getCheckedState(id)
            v.checkBox.isChecked = isChecked
        }
        alternateColorIfNeeded(v, context, cursor)
    }

    protected fun alternateColorIfNeeded(v: BlotterViewHolder, context: Context, cursor: Cursor) {
        if (MyPreferences.isAccountAlternateColors(context) && cursor.position % 2 == 1) {
            v.layout.setBackgroundColor(context.resources.getColor(R.color.alternate_row))
        } else {
            v.layout.setBackgroundColor(context.resources.getColor(R.color.global_background))
        }
    }

    private fun setTransactionTitleText(cursor: Cursor, noteView: TextView) {
        sb.setLength(0)
        val payee = cursor.getString(BlotterColumns.payee.ordinal)
        val note = cursor.getString(BlotterColumns.note.ordinal)
        val locationId = cursor.getLong(BlotterColumns.location_id.ordinal)
        val location = getLocationTitle(cursor, locationId)
        val categoryId = cursor.getLong(BlotterColumns.category_id.ordinal)
        val category = getCategoryTitle(cursor, categoryId)
        val text = generateTransactionTitle(sb, payee, note, location, categoryId, category)
        noteView.text = text
    }

    private fun getCategoryTitle(cursor: Cursor, categoryId: Long): String {
        var category = ""
        if (categoryId != 0L) {
            category = cursor.getString(BlotterColumns.category_title.ordinal)
        }
        return category
    }

    private fun getLocationTitle(cursor: Cursor, locationId: Long): String {
        var location = ""
        if (locationId > 0) {
            location = cursor.getString(BlotterColumns.location.ordinal)
        }
        return location
    }

    fun removeRightViewIfNeeded(v: BlotterViewHolder) {
        if (v.rightView != null && !isShowRunningBalance) {
            v.rightView.visibility = View.GONE
        }
    }

    fun setIndicatorColor(v: BlotterViewHolder, cursor: Cursor) {
        val status = TransactionStatus.valueOf(cursor.getString(BlotterColumns.status.ordinal))
        v.indicator.setBackgroundColor(colors[status.ordinal])
    }

    private fun getCheckedState(id: Long): Boolean {
        return checkedItems[id] == null == allChecked
    }

    private fun updateCheckedState(id: Long, checked: Boolean) {
        if (checked) {
            checkedItems[id] = true
        } else {
            checkedItems.remove(id)
        }
    }

    val checkedCount: Int
        get() = if (allChecked) count - checkedItems.size else checkedItems.size

    fun checkAll() {
        allChecked = true
        checkedItems.clear()
        notifyDataSetInvalidated()
    }

    fun uncheckAll() {
        allChecked = false
        checkedItems.clear()
        notifyDataSetInvalidated()
    }

    class BlotterViewHolder(view: View) {
        @JvmField
        val layout: RelativeLayout
        @JvmField
        val indicator: TextView
        @JvmField
        val topView: TextView
        @JvmField
        val centerView: TextView
        @JvmField
        val bottomView: TextView
        @JvmField
        val rightCenterView: TextView
        @JvmField
        val rightView: TextView?
        @JvmField
        val iconView: ImageView
        val checkBox: CheckBox?

        init {
            layout = view.findViewById(R.id.layout)
            indicator = view.findViewById(R.id.indicator)
            topView = view.findViewById(R.id.top)
            centerView = view.findViewById(R.id.center)
            bottomView = view.findViewById(R.id.bottom)
            rightCenterView = view.findViewById(R.id.right_center)
            rightView = view.findViewById(R.id.right)
            iconView = view.findViewById(R.id.right_top)
            checkBox = view.findViewById(R.id.cb)
        }
    }

    val allCheckedIds: LongArray
        get() {
            val checkedCount = checkedCount
            val ids = LongArray(checkedCount)
            var k = 0
            if (allChecked) {
                val count = count
                val addAll = count == checkedCount
                for (i in 0 until count) {
                    val id = getItemId(i)
                    val checked = addAll || getCheckedState(id)
                    if (checked) {
                        ids[k++] = id
                    }
                }
            } else {
                for (id in checkedItems.keys) {
                    ids[k++] = id
                }
            }
            return ids
        }

    init {
        colors = initializeColors(context)
        isShowRunningBalance = MyPreferences.isShowRunningBalance(context)
        this.db = db
    }
}