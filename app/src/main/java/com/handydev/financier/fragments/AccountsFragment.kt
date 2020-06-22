package com.handydev.financier.fragments

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ImageButton
import android.widget.ListAdapter
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import com.handydev.financier.R
import com.handydev.financier.activity.*
import com.handydev.financier.adapter.AccountListAdapter2
import com.handydev.financier.blotter.BlotterFilter
import com.handydev.financier.blotter.TotalCalculationTask
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.dialog.AccountInfoDialog
import com.handydev.financier.filter.Criteria
import com.handydev.financier.model.Account
import com.handydev.financier.model.Total
import com.handydev.financier.utils.IntegrityCheckAutobackup
import com.handydev.financier.utils.MenuItemInfo
import com.handydev.financier.utils.MyPreferences
import com.handydev.financier.view.NodeInflater
import com.handydev.financier.base.AbstractListFragment
import greendroid.widget.QuickActionGrid
import greendroid.widget.QuickActionWidget
import greendroid.widget.QuickActionWidget.OnQuickActionClickListener
import java.util.*
import java.util.concurrent.TimeUnit

open class AccountsFragment: AbstractListFragment(R.layout.account_list) {
    companion object {
        @JvmStatic val NEW_ACCOUNT_REQUEST = 1
        @JvmStatic val EDIT_ACCOUNT_REQUEST = 2
        @JvmStatic val VIEW_ACCOUNT_REQUEST = 3
        @JvmStatic val PURGE_ACCOUNT_REQUEST = 4
    }
    private var accountActionGrid: QuickActionWidget? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setupUi()
        setupMenuButton()
        calculateTotals()
        integrityCheck()
    }

    override fun onResume() {
        super.onResume()
        recreateCursor()
    }

    private fun setupUi() {
        view?.findViewById<View>(R.id.integrity_error)?.setOnClickListener { v: View -> v.visibility = View.GONE }
        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, view, _, id ->
            selectedId = id
            prepareAccountActionGrid()
            accountActionGrid!!.show(view)
            true
        }
    }

    private fun setupMenuButton() {
        if(null == activity) {
            return
        }
        val bMenu = view?.findViewById<ImageButton>(R.id.bMenu)
        if (MyPreferences.isShowMenuButtonOnAccountsScreen(activity!!)) {
            bMenu?.setOnClickListener { v: View? ->
                val popupMenu = PopupMenu(activity!!, bMenu)
                val inflater = activity!!.menuInflater
                inflater.inflate(R.menu.account_list_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    handlePopupMenu(item.itemId)
                    true
                }
                popupMenu.show()
            }
        } else {
            bMenu?.visibility = View.GONE
        }
    }

    private fun handlePopupMenu(id: Int) {
        if(null == activity) {
            return
        }
        when (id) {
            R.id.backup -> MenuListItem.MENU_BACKUP.call(activity!!)
        }
    }

    private fun prepareAccountActionGrid() {
        if(activity == null || db == null) {
            return
        }
        val a: Account = db!!.getAccount(selectedId)
        accountActionGrid = QuickActionGrid(activity!!)
        accountActionGrid?.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_info, R.string.info))
        accountActionGrid?.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_list, R.string.blotter))
        accountActionGrid?.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_edit, R.string.edit))
        accountActionGrid?.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_add, R.string.transaction))
        accountActionGrid?.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_transfer, R.string.transfer))
        accountActionGrid?.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_tick, R.string.balance))
        accountActionGrid?.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_flash, R.string.delete_old_transactions))
        if (a.isActive) {
            accountActionGrid?.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_lock_closed, R.string.close_account))
        } else {
            accountActionGrid?.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_lock_open, R.string.reopen_account))
        }
        accountActionGrid?.addQuickAction(MyQuickAction(activity!!, R.drawable.ic_action_trash, R.string.delete_account))
        accountActionGrid?.setOnQuickActionClickListener(accountActionListener)
    }

    private val accountActionListener = OnQuickActionClickListener { _, position ->
        when (position) {
            0 -> showAccountInfo(selectedId)
            1 -> showAccountTransactions(selectedId)
            2 -> editAccount(selectedId)
            3 -> addTransaction(selectedId, TransactionActivity::class.java)
            4 -> addTransaction(selectedId, TransferActivity::class.java)
            5 -> updateAccountBalance(selectedId)
            6 -> purgeAccount()
            7 -> closeOrOpenAccount()
            8 -> deleteAccount()
        }
    }

    private fun addTransaction(accountId: Long, clazz: Class<out AbstractTransactionActivity?>) {
        if(activity == null) {
            return
        }
        val intent = Intent(activity!!, clazz)
        intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, accountId)
        startActivityForResult(intent, VIEW_ACCOUNT_REQUEST)
    }

    override fun recreateCursor() {
        super.recreateCursor()
        calculateTotals()
    }

    private var totalCalculationTask: AccountTotalsCalculationTask? = null

    private fun calculateTotals() {
        if(activity == null || db == null) {
            return
        }
        if (totalCalculationTask != null) {
            totalCalculationTask!!.stop()
            totalCalculationTask!!.cancel(true)
        }
        val totalText = view?.findViewById<TextView>(R.id.total)
        totalText?.setOnClickListener { showTotals() }
        totalCalculationTask = AccountTotalsCalculationTask(activity!!, db!!, totalText)
        totalCalculationTask!!.execute()
    }

    private fun showTotals() {
        if(activity == null) {
            return
        }
        val intent = Intent(activity!!, AccountListTotalsDetailsActivity::class.java)
        startActivityForResult(intent, -1)
    }

    class AccountTotalsCalculationTask internal constructor(context: Context?, private val db: DatabaseAdapter, totalText: TextView?) : TotalCalculationTask(context, totalText) {
        override fun getTotalInHomeCurrency(): Total {
            return db.accountsTotalInHomeCurrency
        }
        override fun getTotals(): Array<Total?> {
            return arrayOfNulls(0)
        }
    }

    override fun createAdapter(cursor: Cursor?): ListAdapter? {
        if(activity == null) {
            return null
        }
        return AccountListAdapter2(activity!!, cursor)
    }

    override fun createCursor(): Cursor? {
        if(activity != null) {
            return if (MyPreferences.isHideClosedAccounts(activity!!)) {
                db?.allActiveAccounts
            } else {
                db?.allAccounts
            }
        }
        return null
    }

    override fun createContextMenus(id: Long): ArrayList<MenuItemInfo> {
        return ArrayList<MenuItemInfo>()
    }

    override fun onPopupItemSelected(itemId: Int, view: View?, position: Int, id: Long): Boolean {
        // do nothing
        return true
    }

    private fun updateAccountBalance(id: Long): Boolean {
        val a = db?.getAccount(id)
        if (a != null && activity != null) {
            val intent = Intent(activity!!, TransactionActivity::class.java)
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, a.id)
            intent.putExtra(TransactionActivity.CURRENT_BALANCE_EXTRA, a.totalAmount)
            startActivityForResult(intent, 0)
            return true
        }
        return false
    }

    override fun addItem() {
        if(activity == null) {
            return
        }
        val intent = Intent(activity!!, AccountActivity::class.java)
        startActivityForResult(intent, NEW_ACCOUNT_REQUEST)
    }

    override fun deleteItem(v: View?, position: Int, id: Long) {
        if(activity == null) {
            return
        }
        AlertDialog.Builder(activity!!)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.yes) { _, _ ->
                    db?.deleteAccount(id)
                    recreateCursor()
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    override fun editItem(v: View?, position: Int, id: Long) {
        editAccount(id)
    }

    private fun editAccount(id: Long) {
        if(activity == null) {
            return
        }
        val intent = Intent(activity!!, AccountActivity::class.java)
        intent.putExtra(AccountActivity.ACCOUNT_ID_EXTRA, id)
        startActivityForResult(intent, EDIT_ACCOUNT_REQUEST)
    }

    private var selectedId: Long = -1

    private fun showAccountInfo(id: Long) {
        if(activity == null) {
            return
        }
        val layoutInflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val inflater = NodeInflater(layoutInflater)
        val accountInfoDialog = AccountInfoDialog(activity!!, id, db, inflater)
        accountInfoDialog.show()
    }


    override fun onItemClick(v: View?, position: Int, id: Long) {
        if(activity == null) {
            return
        }
        if (MyPreferences.isQuickMenuEnabledForAccount(activity!!)) {
            selectedId = id
            prepareAccountActionGrid()
            accountActionGrid!!.show(v)
        } else {
            showAccountTransactions(id)
        }
    }

    override fun viewItem(v: View?, position: Int, id: Long) {
        showAccountTransactions(id)
    }

    private fun showAccountTransactions(id: Long) {
        val account = db?.getAccount(id)
        if (account != null && activity != null) {
            val intent = Intent(activity!!, BlotterActivity2::class.java)
            Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, id.toString())
                    .toIntent(account.title, intent)
            intent.putExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, true)
            activity!!.startActivityForResult(intent, VIEW_ACCOUNT_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val unmaskedRequestCode = requestCode and 0x0000ffff
        if (unmaskedRequestCode == NEW_ACCOUNT_REQUEST || unmaskedRequestCode == VIEW_ACCOUNT_REQUEST || unmaskedRequestCode == PURGE_ACCOUNT_REQUEST || unmaskedRequestCode == EDIT_ACCOUNT_REQUEST) {
            recreateCursor()
        }
    }

    private fun purgeAccount() {
        if(activity == null) {
            return
        }
        val intent = Intent(activity!!, PurgeAccountActivity::class.java)
        intent.putExtra(PurgeAccountActivity.ACCOUNT_ID, selectedId)
        startActivityForResult(intent, PURGE_ACCOUNT_REQUEST)
    }

    private fun closeOrOpenAccount() {
        val a = db?.getAccount(selectedId)
        if(activity == null || a == null) {
            return
        }
        if (a.isActive) {
            AlertDialog.Builder(activity!!)
                    .setMessage(R.string.close_account_confirm)
                    .setPositiveButton(R.string.yes) { _, _ -> flipAccountActive(a) }
                    .setNegativeButton(R.string.no, null)
                    .show()
        } else {
            flipAccountActive(a)
        }
    }

    private fun flipAccountActive(a: Account) {
        a.isActive = !a.isActive
        db?.saveAccount(a)
        recreateCursor()
    }

    private fun deleteAccount() {
        if(activity == null) {
            return
        }
        AlertDialog.Builder(activity!!)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.yes) { _, _ ->
                    db?.deleteAccount(selectedId)
                    recreateCursor()
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    override fun integrityCheck() {
        if(activity == null) {
            return
        }
        IntegrityCheckTask(activity!!).execute(IntegrityCheckAutobackup(activity!!, TimeUnit.DAYS.toMillis(7)))
    }
}