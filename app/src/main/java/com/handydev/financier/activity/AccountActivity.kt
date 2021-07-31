/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 * Contributors:
 * Denis Solonenko - initial API and implementation
 * Abdsandryk - adding bill filtering parameters
 */
package com.handydev.financier.activity

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.InputFilter
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.handydev.financier.R
import com.handydev.financier.adapter.EntityEnumAdapter
import com.handydev.financier.model.*
import com.handydev.financier.utils.EntityEnum
import com.handydev.financier.utils.EnumUtils
import com.handydev.financier.utils.TransactionUtils.createCurrencyAdapter
import com.handydev.financier.utils.Utils
import com.handydev.financier.widget.AmountInput
import com.handydev.financier.widget.AmountInput_
import kotlin.math.abs

class AccountActivity : AbstractActivity() {
    private var amountInput: AmountInput? = null
    private var limitInput: AmountInput? = null
    private var limitAmountView: View? = null
    private var accountTitle: EditText? = null
    private var currencyCursor: Cursor? = null
    private var currencyText: TextView? = null
    private var accountTypeNode: View? = null
    private var cardIssuerNode: View? = null
    private var electronicPaymentNode: View? = null
    private var issuerNode: View? = null
    private var numberText: EditText? = null
    private var numberNode: View? = null
    private var issuerName: EditText? = null
    private var sortOrderText: EditText? = null
    private var isIncludedIntoTotals: CheckBox? = null
    private var noteText: EditText? = null
    private var closingDayText: EditText? = null
    private var paymentDayText: EditText? = null
    private var closingDayNode: View? = null
    private var paymentDayNode: View? = null
    private var accountTypeAdapter: EntityEnumAdapter<AccountType>? = null
    private var cardIssuerAdapter: EntityEnumAdapter<CardIssuer>? = null
    private var electronicPaymentAdapter: EntityEnumAdapter<ElectronicPaymentType>? = null
    private var currencyAdapter: ListAdapter? = null
    private var account: Account? = Account()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.account)
        accountTitle = EditText(this)
        accountTitle!!.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        accountTitle!!.setSingleLine()
        issuerName = EditText(this)
        issuerName!!.setSingleLine()
        numberText = EditText(this)
        numberText!!.setHint(R.string.card_number_hint)
        numberText!!.setSingleLine()
        sortOrderText = EditText(this)
        sortOrderText!!.inputType = InputType.TYPE_CLASS_NUMBER
        sortOrderText!!.filters = arrayOf<InputFilter>(LengthFilter(3))
        sortOrderText!!.setSingleLine()
        closingDayText = EditText(this)
        closingDayText!!.inputType = InputType.TYPE_CLASS_NUMBER
        closingDayText!!.setHint(R.string.closing_day_hint)
        closingDayText!!.setSingleLine()
        paymentDayText = EditText(this)
        paymentDayText!!.inputType = InputType.TYPE_CLASS_NUMBER
        paymentDayText!!.setHint(R.string.payment_day_hint)
        paymentDayText!!.setSingleLine()
        amountInput = AmountInput_.build(this)
        amountInput?.setOwner(this)
        limitInput = AmountInput_.build(this)
        limitInput?.setOwner(this)

        val layout = findViewById<LinearLayout>(R.id.layout)
        accountTypeAdapter = EntityEnumAdapter(this, AccountType.values(), false)
        accountTypeNode = activityLayout.addListNodeIcon(
            layout,
            R.id.account_type,
            R.string.account_type,
            R.string.account_type
        )

        val icon = accountTypeNode!!.findViewById<ImageView>(R.id.icon)
        icon.setColorFilter(ContextCompat.getColor(this, R.color.holo_gray_light))
        cardIssuerAdapter = EntityEnumAdapter(this, CardIssuer.values(), false)
        cardIssuerNode = activityLayout.addListNodeIcon(
            layout,
            R.id.card_issuer,
            R.string.card_issuer,
            R.string.card_issuer
        )
        setVisibility(cardIssuerNode, View.GONE)
        electronicPaymentAdapter = EntityEnumAdapter(this, ElectronicPaymentType.values(), false)
        electronicPaymentNode = activityLayout.addListNodeIcon(
            layout,
            R.id.electronic_payment_type,
            R.string.electronic_payment_type,
            R.string.card_issuer
        )
        setVisibility(electronicPaymentNode, View.GONE)
        issuerNode = activityLayout.addEditNode(layout, R.string.issuer, issuerName)
        setVisibility(issuerNode, View.GONE)
        numberNode = activityLayout.addEditNode(layout, R.string.card_number, numberText)
        setVisibility(numberNode, View.GONE)
        closingDayNode = activityLayout.addEditNode(layout, R.string.closing_day, closingDayText)
        setVisibility(closingDayNode, View.GONE)
        paymentDayNode = activityLayout.addEditNode(layout, R.string.payment_day, paymentDayText)
        setVisibility(paymentDayNode, View.GONE)
        currencyCursor = db.getAllCurrencies("name")
        startManagingCursor(currencyCursor)
        currencyAdapter = createCurrencyAdapter(this, currencyCursor)
        activityLayout.addEditNode(layout, R.string.title, accountTitle)
        currencyText = activityLayout.addListNodePlus(
            layout,
            R.id.currency,
            R.id.currency_add,
            R.string.currency,
            R.string.select_currency
        )
        limitInput?.setExpense()
        limitInput?.disableIncomeExpenseButton()
        limitAmountView = activityLayout.addEditNode(layout, R.string.limit_amount, limitInput)
        setVisibility(limitAmountView, View.GONE)

        val intent = intent
        if (intent != null) {
            val accountId = intent.getLongExtra(ACCOUNT_ID_EXTRA, -1)
            if (accountId != -1L) {
                account = db.getAccount(accountId)
                if (account == null) {
                    account = Account()
                }
            } else {
                selectAccountType(AccountType.valueOf(account!!.type))
            }
        }
        if (account!!.id == -1L) {
            activityLayout.addEditNode(layout, R.string.opening_amount, amountInput)
            amountInput?.setIncome()
        }

        noteText = EditText(this)
        noteText!!.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        noteText!!.setLines(2)
        activityLayout.addEditNode(layout, R.string.note, noteText)
        activityLayout.addEditNode(layout, R.string.sort_order, sortOrderText)
        isIncludedIntoTotals = activityLayout.addCheckboxNode(
            layout,
            R.id.is_included_into_totals, R.string.is_included_into_totals,
            R.string.is_included_into_totals_summary, true
        )
        if (account!!.id > 0) {
            editAccount()
        }

        val bOK = findViewById<Button>(R.id.bOK)
        bOK.setOnClickListener {
            if (account!!.currency == null) {
                Toast.makeText(this@AccountActivity, R.string.select_currency, Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            if (Utils.isEmpty(accountTitle)) {
                accountTitle!!.error = getString(R.string.title)
                return@setOnClickListener
            }
            val type = AccountType.valueOf(
                account!!.type
            )
            if (type.hasIssuer) {
                account!!.issuer = Utils.text(issuerName)
            }
            if (type.hasNumber) {
                account!!.number = Utils.text(numberText)
            }
            /********** validate closing and payment days  */
            if (type.isCreditCard) {
                val closingDay = Utils.text(closingDayText)
                account!!.closingDay = closingDay?.toInt() ?: 0
                if (account!!.closingDay != 0) {
                    if (account!!.closingDay > 31) {
                        Toast.makeText(
                            this@AccountActivity,
                            R.string.closing_day_error,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                }
                val paymentDay = Utils.text(paymentDayText)
                account!!.paymentDay = paymentDay?.toInt() ?: 0
                if (account!!.paymentDay != 0) {
                    if (account!!.paymentDay > 31) {
                        Toast.makeText(
                            this@AccountActivity,
                            R.string.payment_day_error,
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }
                }
            }

            account!!.title = Utils.text(accountTitle)
            account!!.creationDate = System.currentTimeMillis()
            val sortOrder = Utils.text(sortOrderText)
            account!!.sortOrder = sortOrder?.toInt() ?: 0
            account!!.isIncludeIntoTotals = isIncludedIntoTotals!!.isChecked
            account!!.limitAmount = -abs(limitInput?.amount ?: 0)
            account!!.note = Utils.text(noteText)
            val accountId = db.saveAccount(account)
            val amount = amountInput?.amount ?: 0
            if (amount != 0L) {
                val t = Transaction()
                t.fromAccountId = accountId
                t.categoryId = 0
                t.note = resources.getText(R.string.opening_amount)
                    .toString() + " (" + account!!.title + ")"
                t.fromAmount = amount
                db.insertOrUpdate(t, null)
            }
            AccountWidget.updateWidgets(this)
            val intent1 = Intent()
            intent1.putExtra(ACCOUNT_ID_EXTRA, accountId)
            setResult(RESULT_OK, intent1)
            finish()
        }
        val bCancel = findViewById<Button>(R.id.bCancel)
        bCancel.setOnClickListener { arg0: View? ->
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onClick(v: View, id: Int) {
        when (id) {
            R.id.is_included_into_totals -> isIncludedIntoTotals!!.performClick()
            R.id.account_type -> activityLayout.selectPosition(
                this,
                R.id.account_type,
                R.string.account_type,
                accountTypeAdapter!!,
                AccountType.valueOf(
                    account!!.type
                ).ordinal
            )
            R.id.card_issuer -> activityLayout.selectPosition(
                this, R.id.card_issuer, R.string.card_issuer, cardIssuerAdapter!!,
                if (account!!.cardIssuer != null) CardIssuer.valueOf(account!!.cardIssuer).ordinal else 0
            )
            R.id.electronic_payment_type -> activityLayout.selectPosition(
                this,
                R.id.electronic_payment_type,
                R.string.electronic_payment_type,
                electronicPaymentAdapter!!,
                EnumUtils.selectEnum(
                    ElectronicPaymentType::class.java,
                    account!!.cardIssuer,
                    ElectronicPaymentType.PAYPAL
                ).ordinal
            )
            R.id.currency -> {
                if((currencyCursor?.count ?: 0) > 0) {
                    activityLayout.select(
                        this, R.id.currency, R.string.currency, currencyCursor!!, currencyAdapter!!,
                        "_id", if (account!!.currency != null) account!!.currency.id else -1
                    )
                } else {
                    addNewCurrency()
                }
            }
            R.id.currency_add -> addNewCurrency()
        }
    }

    private fun addNewCurrency() {
        CurrencySelector(this, db) { currencyId: Long ->
            if (currencyId == 0L) {
                val intent = Intent(this@AccountActivity, CurrencyActivity::class.java)
                startActivityForResult(intent, NEW_CURRENCY_REQUEST)
            } else {
                currencyCursor!!.requery()
                selectCurrency(currencyId)
            }
        }.show()
    }

    override fun onSelectedId(id: Int, selectedId: Long) {
        when (id) {
            R.id.currency -> selectCurrency(selectedId)
        }
    }

    override fun onSelectedPos(id: Int, selectedPos: Int) {
        when (id) {
            R.id.account_type -> {
                val type = AccountType.values()[selectedPos]
                selectAccountType(type)
            }
            R.id.card_issuer -> {
                val issuer = CardIssuer.values()[selectedPos]
                selectCardIssuer(issuer)
            }
            R.id.electronic_payment_type -> {
                val paymentType = ElectronicPaymentType.values()[selectedPos]
                selectElectronicType(paymentType)
            }
        }
    }

    private fun selectAccountType(type: AccountType) {
        val icon = accountTypeNode!!.findViewById<ImageView>(R.id.icon)
        icon.setImageResource(type.iconId)
        val label = accountTypeNode!!.findViewById<TextView>(R.id.label)
        label.setText(type.titleId)
        setVisibility(cardIssuerNode, if (type.isCard) View.VISIBLE else View.GONE)
        setVisibility(issuerNode, if (type.hasIssuer) View.VISIBLE else View.GONE)
        setVisibility(electronicPaymentNode, if (type.isElectronic) View.VISIBLE else View.GONE)
        setVisibility(numberNode, if (type.hasNumber) View.VISIBLE else View.GONE)
        setVisibility(closingDayNode, if (type.isCreditCard) View.VISIBLE else View.GONE)
        setVisibility(paymentDayNode, if (type.isCreditCard) View.VISIBLE else View.GONE)
        setVisibility(
            limitAmountView,
            if (type == AccountType.CREDIT_CARD) View.VISIBLE else View.GONE
        )
        account!!.type = type.name
        when {
            type.isCard -> {
                selectCardIssuer(
                    EnumUtils.selectEnum(
                        CardIssuer::class.java,
                        account!!.cardIssuer,
                        CardIssuer.DEFAULT
                    )
                )
            }
            type.isElectronic -> {
                selectElectronicType(
                    EnumUtils.selectEnum(
                        ElectronicPaymentType::class.java,
                        account!!.cardIssuer,
                        ElectronicPaymentType.PAYPAL
                    )
                )
            }
            else -> {
                account!!.cardIssuer = null
            }
        }
    }

    private fun selectCardIssuer(issuer: CardIssuer) {
        updateNode(cardIssuerNode, issuer)
        account!!.cardIssuer = issuer.name
    }

    private fun selectElectronicType(paymentType: ElectronicPaymentType) {
        updateNode(electronicPaymentNode, paymentType)
        account!!.cardIssuer = paymentType.name
    }

    private fun updateNode(note: View?, enumItem: EntityEnum) {
        val icon = note!!.findViewById<ImageView>(R.id.icon)
        icon.setImageResource(enumItem.iconId)
        val label = note.findViewById<TextView>(R.id.label)
        label.setText(enumItem.titleId)
    }

    private fun selectCurrency(currencyId: Long) {
        val c = db.get(
            Currency::class.java, currencyId
        )
        c?.let { selectCurrency(it) }
    }

    private fun selectCurrency(c: Currency) {
        currencyText!!.text = c.name
        amountInput!!.currency = c
        limitInput!!.currency = c
        account!!.currency = c
    }

    private fun editAccount() {
        selectAccountType(AccountType.valueOf(account!!.type))
        selectCurrency(account!!.currency)
        accountTitle!!.setText(account!!.title)
        issuerName!!.setText(account!!.issuer)
        numberText!!.setText(account!!.number)
        sortOrderText!!.setText(account!!.sortOrder.toString())
        /******** bill filtering  */
        if (account!!.closingDay > 0) {
            closingDayText!!.setText(account!!.closingDay.toString())
        }
        if (account!!.paymentDay > 0) {
            paymentDayText!!.setText(account!!.paymentDay.toString())
        }
        /** */
        isIncludedIntoTotals!!.isChecked = account!!.isIncludeIntoTotals
        if (account!!.limitAmount != 0L) {
            limitInput!!.amount = -abs(account!!.limitAmount)
        }
        noteText!!.setText(account!!.note)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                NEW_CURRENCY_REQUEST -> {
                    currencyCursor!!.requery()
                    val currencyId = data!!.getLongExtra(CurrencyActivity.CURRENCY_ID_EXTRA, -1)
                    if (currencyId != -1L) {
                        selectCurrency(currencyId)
                    }
                }
            }
        }
    }

    companion object {
        const val ACCOUNT_ID_EXTRA = "accountId"
        private const val NEW_CURRENCY_REQUEST = 1
    }
}