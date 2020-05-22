package com.handydev.main.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.Cursor
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.InputMethodManager
import android.widget.*
import greendroid.widget.QuickActionGrid
import greendroid.widget.QuickActionWidget
import greendroid.widget.QuickActionWidget.OnQuickActionClickListener
import com.handydev.financier.R
import com.handydev.financier.activity.*
import com.handydev.financier.adapter.BlotterListAdapter
import com.handydev.financier.adapter.TransactionsListAdapter
import com.handydev.financier.blotter.AccountTotalCalculationTask
import com.handydev.financier.blotter.BlotterFilter
import com.handydev.financier.blotter.BlotterTotalCalculationTask
import com.handydev.financier.blotter.TotalCalculationTask
import com.handydev.financier.dialog.TransactionInfoDialog
import com.handydev.financier.filter.WhereFilter
import com.handydev.financier.model.AccountType
import com.handydev.financier.utils.IntegrityCheckRunningBalance
import com.handydev.financier.utils.MenuItemInfo
import com.handydev.financier.utils.MyPreferences
import com.handydev.financier.view.NodeInflater
import com.handydev.main.base.AbstractListFragment
import com.handydev.main.protocol.IOTransactionDeleteListener

open class BlotterFragment: AbstractListFragment(R.layout.blotter), IOTransactionDeleteListener {
    companion object {
        const val SAVE_FILTER = "saveFilter"
        private const val NEW_TRANSACTION_REQUEST = 1
        private const val NEW_TRANSFER_REQUEST = 3
        private const val NEW_TRANSACTION_FROM_TEMPLATE_REQUEST = 5
        private const val MONTHLY_VIEW_REQUEST = 6
        private const val BILL_PREVIEW_REQUEST = 7
        protected const val FILTER_REQUEST = 6
    }

    private val MENU_DUPLICATE = MENU_ADD + 1
    private val MENU_SAVE_AS_TEMPLATE = MENU_ADD + 2

    private var totalText: TextView? = null
    private var bFilter: ImageButton? = null
    private var bTransfer: ImageButton? = null
    private var bTemplate: ImageButton? = null
    private var bSearch: ImageButton? = null
    private var bMenu: ImageButton? = null

    private var transactionActionGrid: QuickActionGrid? = null
    private var addButtonActionGrid: QuickActionGrid? = null

    private var calculationTask: TotalCalculationTask? = null

    private var saveFilter = false
    private var blotterFilter = WhereFilter.empty()

    private var isAccountBlotter = false
    private var showAllBlotterButtons = true

    protected fun calculateTotals() {
        if (calculationTask != null) {
            calculationTask!!.stop()
            calculationTask!!.cancel(true)
        }
        calculationTask = createTotalCalculationTask()
        calculationTask!!.execute()
    }

    protected fun createTotalCalculationTask(): TotalCalculationTask? {
        if(activity == null) {
            return null
        }
        val filter = WhereFilter.copyOf(blotterFilter)
        return if (filter.accountId > 0) {
            AccountTotalCalculationTask(activity!!, db, filter, totalText)
        } else {
            BlotterTotalCalculationTask(activity!!, db, filter, totalText)
        }
    }

    override fun recreateCursor() {
        super.recreateCursor()
        calculateTotals()
    }

    override fun internalOnCreate(savedInstanceState: Bundle?) {
        super.internalOnCreate(savedInstanceState)
        integrityCheck()
        bFilter = view?.findViewById(R.id.bFilter)
        bFilter!!.setOnClickListener {
            val intent = Intent(activity!!, BlotterFilterActivity::class.java)
            blotterFilter.toIntent(intent)
            intent.putExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, isAccountBlotter && blotterFilter.accountId > 0)
            startActivityForResult(intent, FILTER_REQUEST)
        }
        totalText = view?.findViewById<TextView>(R.id.total)
        totalText!!.setOnClickListener { showTotals() }
        val intent: Intent = activity!!.intent
        blotterFilter = WhereFilter.fromIntent(intent)
        saveFilter = arguments?.getBoolean(SAVE_FILTER) ?: false
        isAccountBlotter = intent.getBooleanExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, false)
        if (savedInstanceState != null) {
            blotterFilter = WhereFilter.fromBundle(savedInstanceState)
        }
        if (saveFilter && blotterFilter.isEmpty) {
            blotterFilter = WhereFilter.fromSharedPreferences(activity!!.getPreferences(0))
        }
        showAllBlotterButtons = !isAccountBlotter && !MyPreferences.isCollapseBlotterButtons(activity!!)
        if (showAllBlotterButtons) {
            bTransfer = view?.findViewById(R.id.bTransfer)
            bTransfer!!.visibility = View.VISIBLE
            bTransfer!!.setOnClickListener { addItem(NEW_TRANSFER_REQUEST, TransferActivity::class.java) }
            bTemplate = view?.findViewById(R.id.bTemplate)
            bTemplate!!.visibility = View.VISIBLE
            bTemplate!!.setOnClickListener { createFromTemplate() }
        }
        bSearch = view?.findViewById<ImageButton>(R.id.bSearch)
        bSearch!!.setOnClickListener {
            val searchText = view?.findViewById<EditText>(R.id.search_text)
            val searchLayout = view?.findViewById<FrameLayout>(R.id.search_text_frame)
            val searchTextClearButton = view?.findViewById<ImageButton>(R.id.search_text_clear)
            val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            searchText?.onFocusChangeListener = OnFocusChangeListener { view: View, b: Boolean ->
                if (!view.hasFocus()) {
                    imm.hideSoftInputFromWindow(searchLayout?.windowToken, 0)
                }
            }
            searchTextClearButton?.setOnClickListener { searchText?.setText("") }
            if (searchLayout?.visibility == View.VISIBLE) {
                imm.hideSoftInputFromWindow(searchLayout.windowToken, 0)
                searchLayout.visibility = View.GONE
                return@setOnClickListener
            }
            searchLayout?.visibility = View.VISIBLE
            searchText?.requestFocusFromTouch()
            imm.showSoftInput(searchText, InputMethodManager.SHOW_IMPLICIT)
            searchText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
                override fun afterTextChanged(editable: Editable) {
                    val clearButton = view?.findViewById<ImageButton>(R.id.search_text_clear)
                    val text = editable.toString()
                    blotterFilter.remove(BlotterFilter.NOTE)
                    if (text.isNotEmpty()) {
                        blotterFilter.contains(BlotterFilter.NOTE, text)
                        clearButton?.visibility = View.VISIBLE
                    } else {
                        clearButton?.visibility = View.GONE
                    }
                    recreateCursor()
                    applyFilter()
                    saveFilter()
                }
            })
            if (blotterFilter[BlotterFilter.NOTE] != null) {
                var searchFilterText = blotterFilter[BlotterFilter.NOTE].stringValue
                if (searchFilterText.isNotEmpty()) {
                    searchFilterText = searchFilterText.substring(1, searchFilterText.length - 1)
                    searchText?.setText(searchFilterText)
                }
            }
        }
        applyFilter()
        applyPopupMenu()
        calculateTotals()
        prepareTransactionActionGrid()
        prepareAddButtonActionGrid()
    }

    private fun applyPopupMenu() {
        bMenu = view?.findViewById(R.id.bMenu)
        if (isAccountBlotter && activity != null) {
            bMenu!!.setOnClickListener {
                val popupMenu = PopupMenu(activity!!, bMenu)
                val accountId = blotterFilter.accountId
                if (accountId != -1L) {
                    // get account type
                    val account = db!!.getAccount(accountId)
                    val type = AccountType.valueOf(account.type)
                    if (type.isCreditCard) {
                        // Show menu for Credit Cards - bill
                        val inflater: MenuInflater = activity!!.menuInflater
                        inflater.inflate(R.menu.ccard_blotter_menu, popupMenu.menu)
                    } else {
                        // Show menu for other accounts - monthly view
                        val inflater: MenuInflater = activity!!.menuInflater
                        inflater.inflate(R.menu.blotter_menu, popupMenu.menu)
                    }
                    popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                        onPopupMenuSelected(item.itemId)
                        true
                    }
                    popupMenu.show()
                }
            }
        } else {
            bMenu!!.visibility = View.GONE
        }
    }

    private fun onPopupMenuSelected(id: Int) {
        if(activity == null) {
            return
        }
        val accountId = blotterFilter.accountId
        val intent = Intent(activity!!, MonthlyViewActivity::class.java)
        intent.putExtra(MonthlyViewActivity.ACCOUNT_EXTRA, accountId)
        when (id) {
            R.id.opt_menu_month -> {
                // call credit card bill activity sending account id
                intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, false)
                startActivityForResult(intent, MONTHLY_VIEW_REQUEST)
            }
            R.id.opt_menu_bill -> if (accountId != -1L) {
                val account = db!!.getAccount(accountId)

                // call credit card bill activity sending account id
                if (account.paymentDay > 0 && account.closingDay > 0) {
                    intent.putExtra(MonthlyViewActivity.BILL_PREVIEW_EXTRA, true)
                    startActivityForResult(intent, BILL_PREVIEW_REQUEST)
                } else {
                    // display message: need payment and closing day
                    val dlgAlert = AlertDialog.Builder(activity!!)
                    dlgAlert.setMessage(R.string.statement_error)
                    dlgAlert.setTitle(R.string.ccard_statement)
                    dlgAlert.setPositiveButton(R.string.ok, null)
                    dlgAlert.setCancelable(true)
                    dlgAlert.create().show()
                }
            }
        }
    }

    private fun showTotals() {
        if(activity == null) {
            return
        }
        val intent = Intent(activity!!, BlotterTotalsDetailsActivity::class.java)
        blotterFilter.toIntent(intent)
        startActivityForResult(intent, -1)
    }

    private fun prepareTransactionActionGrid() {
        if(activity == null) {
            return
        }
        transactionActionGrid = QuickActionGrid(activity!!)
        transactionActionGrid!!.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_info, R.string.info))
        transactionActionGrid!!.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_edit, R.string.edit))
        transactionActionGrid!!.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_trash, R.string.delete))
        transactionActionGrid!!.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_copy, R.string.duplicate))
        transactionActionGrid!!.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_tick, R.string.clear))
        transactionActionGrid!!.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_double_tick, R.string.reconcile))
        transactionActionGrid!!.setOnQuickActionClickListener(transactionActionListener)
    }

    private val transactionActionListener = OnQuickActionClickListener { _, position ->
        when (position) {
            0 -> showTransactionInfo(selectedId)
            1 -> editTransaction(selectedId)
            2 -> deleteTransaction(selectedId)
            3 -> duplicateTransaction(selectedId, 1)
            4 -> clearTransaction(selectedId)
            5 -> reconcileTransaction(selectedId)
        }
    }

    private fun prepareAddButtonActionGrid() {
        if(activity == null) {
            return
        }
        addButtonActionGrid = QuickActionGrid(activity!!)
        addButtonActionGrid!!.addQuickAction(MyQuickAction(activity!!, R.drawable.actionbar_add_big, R.string.transaction))
        addButtonActionGrid!!.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_transfer, R.string.transfer))
        if (addTemplateToAddButton()) {
            addButtonActionGrid!!.addQuickAction(MyQuickAction(activity!!, R.drawable.actionbar_tiles_large, R.string.template))
        } else {
            addButtonActionGrid!!.setNumColumns(2)
        }
        addButtonActionGrid!!.setOnQuickActionClickListener(addButtonActionListener)
    }

    private fun addTemplateToAddButton(): Boolean {
        return true
    }

    private val addButtonActionListener = OnQuickActionClickListener { _: QuickActionWidget?, position: Int ->
        when (position) {
            0 -> addItem(NEW_TRANSACTION_REQUEST, TransactionActivity::class.java)
            1 -> addItem(NEW_TRANSFER_REQUEST, TransferActivity::class.java)
            2 -> createFromTemplate()
        }
    }

    private fun clearTransaction(selectedId: Long) {
        if(activity == null) {
            return
        }
        BlotterOperations(activity!!, db, selectedId, this).clearTransaction()
        recreateCursor()
    }

    private fun reconcileTransaction(selectedId: Long) {
        if(activity == null) {
            return
        }
        BlotterOperations(activity!!, db, selectedId, this).reconcileTransaction()
        recreateCursor()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        blotterFilter.toBundle(outState)
    }

    private fun createFromTemplate() {
        if(activity == null) {
            return
        }
        val intent = Intent(activity!!, SelectTemplateActivity::class.java)
        startActivityForResult(intent, NEW_TRANSACTION_FROM_TEMPLATE_REQUEST)
    }

    override fun createContextMenus(id: Long): ArrayList<MenuItemInfo> {
        return if (blotterFilter.isTemplate() || blotterFilter.isSchedule) {
            super.createContextMenus(id)
        } else {
            val menus = super.createContextMenus(id)
            menus.add(MenuItemInfo(MENU_DUPLICATE, R.string.duplicate))
            menus.add(MenuItemInfo(MENU_SAVE_AS_TEMPLATE, R.string.save_as_template))
            menus
        }
    }

    override fun onPopupItemSelected(itemId: Int, view: View?, position: Int, id: Long): Boolean {
        if(activity == null) {
            return false
        }
        if (!super.onPopupItemSelected(itemId, view, position, id)) {
            when (itemId) {
                MENU_DUPLICATE -> {
                    duplicateTransaction(id, 1)
                    return true
                }
                MENU_SAVE_AS_TEMPLATE -> {
                    BlotterOperations(activity!!, db, id, this).duplicateAsTemplate()
                    Toast.makeText(activity!!, R.string.save_as_template_success, Toast.LENGTH_SHORT).show()
                    return true
                }
            }
        }
        return false
    }

    private fun duplicateTransaction(id: Long, multiplier: Int): Long {
        if(activity == null) {
            return 0
        }
        val newId = BlotterOperations(activity!!, db, id, this).duplicateTransaction(multiplier)
        val toastText: String = if (multiplier > 1) {
            getString(R.string.duplicate_success_with_multiplier, multiplier)
        } else {
            getString(R.string.duplicate_success)
        }
        Toast.makeText(activity!!, toastText, Toast.LENGTH_LONG).show()
        recreateCursor()
        AccountWidget.updateWidgets(activity!!)
        return newId
    }

    override fun addItem() {
        if (showAllBlotterButtons) {
            addItem(NEW_TRANSACTION_REQUEST, TransactionActivity::class.java)
        } else {
            addButtonActionGrid!!.show(bAdd)
        }
    }

    protected fun addItem(requestId: Int, clazz: Class<out AbstractTransactionActivity?>?) {
        if(activity == null) {
            return
        }
        val intent = Intent(activity!!, clazz)
        val accountId = blotterFilter.accountId
        if (accountId != -1L) {
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId)
        }
        intent.putExtra(TransactionActivity.TEMPLATE_EXTRA, blotterFilter.getIsTemplate())
        startActivityForResult(intent, requestId)
    }

    override fun createCursor(): Cursor? {
        return if (isAccountBlotter) {
            db!!.getBlotterForAccount(blotterFilter)
        } else {
            db!!.getBlotter(blotterFilter)
        }
    }

    override fun createAdapter(cursor: Cursor?): ListAdapter? {
        if(activity == null) {
            return null
        }
        return if (isAccountBlotter) {
            TransactionsListAdapter(activity!!, db, cursor)
        } else {
            BlotterListAdapter(activity!!, db, cursor)
        }
    }

    override fun deleteItem(v: View?, position: Int, id: Long) {
        deleteTransaction(id)
    }

    private fun deleteTransaction(id: Long) {
        if(activity == null) {
            return
        }
        BlotterOperations(activity!!, db, id, this).deleteTransaction()
    }

    override fun afterDeletingTransaction(id: Long) {
        if(activity == null) {
            return
        }
        recreateCursor()
        AccountWidget.updateWidgets(activity!!)
    }

    override fun editItem(v: View?, position: Int, id: Long) {
        editTransaction(id)
    }

    private fun editTransaction(id: Long) {
        if(activity == null) {
            return
        }
        BlotterOperations(activity!!, db, id, this).editTransaction()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val unmaskedRequestCode = requestCode and 0x0000ffff
        if (unmaskedRequestCode == FILTER_REQUEST) {
            if (unmaskedRequestCode == Activity.RESULT_FIRST_USER) {
                blotterFilter.clear()
            } else if (resultCode == Activity.RESULT_OK) {
                blotterFilter = WhereFilter.fromIntent(data)
            }
            if (saveFilter) {
                saveFilter()
            }
            applyFilter()
            recreateCursor()
        } else if (unmaskedRequestCode == Activity.RESULT_OK && unmaskedRequestCode == NEW_TRANSACTION_FROM_TEMPLATE_REQUEST && data != null) {
            createTransactionFromTemplate(data)
        }
        if (unmaskedRequestCode == Activity.RESULT_OK || unmaskedRequestCode == Activity.RESULT_FIRST_USER) {
            calculateTotals()
        }
    }

    private fun createTransactionFromTemplate(data: Intent) {
        if(activity == null) {
            return
        }
        val templateId = data.getLongExtra(SelectTemplateActivity.TEMPATE_ID, -1)
        val multiplier = data.getIntExtra(SelectTemplateActivity.MULTIPLIER, 1)
        val edit = data.getBooleanExtra(SelectTemplateActivity.EDIT_AFTER_CREATION, false)
        if (templateId > 0) {
            val id = duplicateTransaction(templateId, multiplier)
            val t = db!!.getTransaction(id)
            if (t.fromAmount == 0L || edit) {
                BlotterOperations(activity!!, db, id, this).asNewFromTemplate().editTransaction()
            }
        }
    }

    private fun saveFilter() {
        if(activity == null) {
            return
        }
        val preferences: SharedPreferences = activity!!.getPreferences(0)
        blotterFilter.toSharedPreferences(preferences)
    }

    protected fun applyFilter() {
        val accountId = blotterFilter.accountId
        if (accountId != -1L) {
            val a = db!!.getAccount(accountId)
            bAdd!!.visibility = if (a != null && a.isActive) View.VISIBLE else View.GONE
            if (showAllBlotterButtons) {
                bTransfer!!.visibility = if (a != null && a.isActive) View.VISIBLE else View.GONE
            }
        }
        val title = blotterFilter.title
        if (title != null) {
            //setScreenTitle(getString(R.string.blotter) + " : " + title)
        }
        updateFilterImage()
    }

    /*private fun setScreenTitle(title: String) {
        val container = view?.findViewById<LinearLayout>(R.id.title_view)
        container?.visibility = if(title.isEmpty()) View.GONE else View.VISIBLE
        view?.findViewById<TextView>(R.id.title_view_text)?.text = title
    }*/

    private fun updateFilterImage() {
        if(activity == null) {
            return
        }
        FilterState.updateFilterColor(activity!!, blotterFilter, bFilter)
    }

    private var selectedId: Long = -1

    override fun onItemClick(v: View?, position: Int, id: Long) {
        if(activity == null) {
            return
        }
        if (MyPreferences.isQuickMenuEnabledForTransaction(activity!!)) {
            selectedId = id
            transactionActionGrid!!.show(v)
        } else {
            showTransactionInfo(id)
        }
    }

    override fun viewItem(v: View?, position: Int, id: Long) {
        showTransactionInfo(id)
    }

    private fun showTransactionInfo(id: Long) {
        if(activity == null || inflater == null) {
            return
        }
        val transactionInfoView = TransactionInfoDialog(activity!!, db, NodeInflater(inflater!!))
        transactionInfoView.show(activity!!, id, this)
    }

    override fun integrityCheck() {
        if(activity == null) {
            return
        }
        IntegrityCheckTask(activity!!).execute(IntegrityCheckRunningBalance(activity!!, db))
    }

    override fun onBackPressed(): Boolean {
        if(activity == null) {
            return true
        }
        val searchLayout = view?.findViewById<FrameLayout>(R.id.search_text_frame)
        return if (searchLayout != null && searchLayout.visibility == View.VISIBLE) {
            searchLayout.visibility = View.GONE
            false
        } else {
            true
        }
    }
}