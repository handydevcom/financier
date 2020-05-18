package ru.orangesoftware.main.fragments

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.view.*
import android.widget.*
import android.widget.AdapterView.OnItemLongClickListener
import androidx.fragment.app.Fragment
import greendroid.widget.QuickActionGrid
import greendroid.widget.QuickActionWidget
import greendroid.widget.QuickActionWidget.OnQuickActionClickListener
import ru.orangesoftware.financisto.R
import ru.orangesoftware.financisto.activity.*
import ru.orangesoftware.financisto.adapter.AccountListAdapter2
import ru.orangesoftware.financisto.blotter.BlotterFilter
import ru.orangesoftware.financisto.blotter.TotalCalculationTask
import ru.orangesoftware.financisto.bus.GreenRobotBus_
import ru.orangesoftware.financisto.bus.SwitchToMenuTabEvent
import ru.orangesoftware.financisto.db.DatabaseAdapter
import ru.orangesoftware.financisto.dialog.AccountInfoDialog
import ru.orangesoftware.financisto.filter.Criteria
import ru.orangesoftware.financisto.model.Account
import ru.orangesoftware.financisto.model.Total
import ru.orangesoftware.financisto.utils.IntegrityCheckAutobackup
import ru.orangesoftware.financisto.utils.MenuItemInfo
import ru.orangesoftware.financisto.utils.MyPreferences
import ru.orangesoftware.financisto.view.NodeInflater
import java.util.*
import java.util.concurrent.TimeUnit

class AccountsFragment: Fragment() {
    companion object {
        val NEW_ACCOUNT_REQUEST = 1
        val EDIT_ACCOUNT_REQUEST = 2
        val VIEW_ACCOUNT_REQUEST = 3
        val PURGE_ACCOUNT_REQUEST = 4
    }
    private var accountActionGrid: QuickActionWidget? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.account_list, container, false)
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUi()
        setupMenuButton()
        calculateTotals()
        integrityCheck()
    }

    private fun setupUi() {
        view?.findViewById<View>(R.id.integrity_error)?.setOnClickListener(View.OnClickListener { v: View -> v.visibility = View.GONE })
        /*getListView().setOnItemLongClickListener(OnItemLongClickListener { parent: AdapterView<*>?, view: View?, position: Int, id: Long ->
            selectedId = id
            prepareAccountActionGrid()
            accountActionGrid!!.show(view)
            true
        })*/
    }

    private fun setupMenuButton() {
        if(activity == null) {
            return
        }
        val bMenu = view?.findViewById<ImageButton>(R.id.bMenu)
        if (MyPreferences.isShowMenuButtonOnAccountsScreen(activity!!)) {
            /*bMenu?.setOnClickListener { v: View? ->
                val popupMenu = PopupMenu(this@AccountListActivity, bMenu)
                val inflater: MenuInflater = getMenuInflater()
                inflater.inflate(R.menu.account_list_menu, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    handlePopupMenu(item.itemId)
                    true
                }
                popupMenu.show()
            }*/
        } else {
            bMenu?.visibility = View.GONE
        }
    }

    private fun handlePopupMenu(id: Int) {
        if(activity == null) {
            return
        }
        when (id) {
            R.id.backup -> MenuListItem.MENU_BACKUP.call(activity!!)
            R.id.go_to_menu -> GreenRobotBus_.getInstance_(activity!!).post(SwitchToMenuTabEvent())
        }
    }

    protected fun prepareAccountActionGrid() {
        if(activity == null) {
            return
        }
        /*val a: Account = db.getAccount(selectedId)
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
        accountActionGrid?.setOnQuickActionClickListener(accountActionListener)*/
    }

    private val accountActionListener = OnQuickActionClickListener { widget, position ->
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

    /*fun recreateCursor() {
        super.recreateCursor()
        calculateTotals()
    }*/

    private var totalCalculationTask: AccountTotalsCalculationTask? = null

    private fun calculateTotals() {
        if(activity == null) {
            return
        }
        if (totalCalculationTask != null) {
            totalCalculationTask!!.stop()
            totalCalculationTask!!.cancel(true)
        }
        val totalText = view?.findViewById<TextView>(R.id.total)
        totalText?.setOnClickListener { showTotals() }
        //totalCalculationTask = AccountTotalsCalculationTask(activity!!, db, totalText)
        //totalCalculationTask!!.execute()
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

    protected fun createAdapter(cursor: Cursor?): ListAdapter? {
        if(activity != null) {
            return null
        }
        return AccountListAdapter2(activity!!, cursor)
    }

    protected fun createCursor(): Cursor? {
        /*return if (MyPreferences.isHideClosedAccounts(this)) {
            db.getAllActiveAccounts()
        } else {
            db.getAllAccounts()
        }*/
        return null
    }

    protected fun createContextMenus(id: Long): List<MenuItemInfo?>? {
        return ArrayList()
    }

    fun onPopupItemSelected(itemId: Int, view: View?, position: Int, id: Long): Boolean {
        // do nothing
        return true
    }

    private fun updateAccountBalance(id: Long): Boolean {
        /*val a: Account = db.getAccount(id)
        if (a != null) {
            val intent = Intent(this, TransactionActivity::class.java)
            intent.putExtra(TransactionActivity.ACCOUNT_ID_EXTRA, a.id)
            intent.putExtra(TransactionActivity.CURRENT_BALANCE_EXTRA, a.totalAmount)
            startActivityForResult(intent, 0)
            return true
        }*/
        return false
    }

    protected fun addItem() {
        if(activity == null) {
            return
        }
        val intent = Intent(activity!!, AccountActivity::class.java)
        startActivityForResult(intent, NEW_ACCOUNT_REQUEST)
    }

    protected fun deleteItem(v: View?, position: Int, id: Long) {
        if(activity == null) {
            return
        }
        /*AlertDialog.Builder(activity)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.yes) { arg0, arg1 ->
                    db.deleteAccount(id)
                    recreateCursor()
                }
                .setNegativeButton(R.string.no, null)
                .show()*/
    }

    fun editItem(v: View?, position: Int, id: Long) {
        editAccount(id)
    }

    private fun editAccount(id: Long) {
        if(activity == null) {
            return
        }
        val intent = Intent(activity!!, AccountActivity::class.java)
        intent.putExtra(AccountActivity.ACCOUNT_ID_EXTRA, id)
        startActivityForResult(intent, AccountListActivity.EDIT_ACCOUNT_REQUEST)
    }

    private var selectedId: Long = -1

    private fun showAccountInfo(id: Long) {
        if(activity == null) {
            return
        }
        /*val layoutInflater = activity!!.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val inflater = NodeInflater(layoutInflater)
        val accountInfoDialog = AccountInfoDialog(this, id, db, inflater)
        accountInfoDialog.show()*/
    }


    protected fun onItemClick(v: View?, position: Int, id: Long) {
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

    protected fun viewItem(v: View?, position: Int, id: Long) {
        showAccountTransactions(id)
    }

    private fun showAccountTransactions(id: Long) {
        /*val account: Account = db.getAccount(id)
        if (account != null && activity != null) {
            val intent = Intent(activity!!, BlotterActivity::class.java)
            Criteria.eq(BlotterFilter.FROM_ACCOUNT_ID, id.toString())
                    .toIntent(account.title, intent)
            intent.putExtra(BlotterFilterActivity.IS_ACCOUNT_FILTER, true)
            //startActivityForResult(intent, AccountListActivity.VIEW_ACCOUNT_REQUEST)
        }*/
    }

   /* protected override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AccountListActivity.VIEW_ACCOUNT_REQUEST || requestCode == AccountListActivity.PURGE_ACCOUNT_REQUEST) {
            recreateCursor()
        }
    }*/

    private fun purgeAccount() {
        if(activity == null) {
            return
        }
        val intent = Intent(activity!!, PurgeAccountActivity::class.java)
        intent.putExtra(PurgeAccountActivity.ACCOUNT_ID, selectedId)
        startActivityForResult(intent, PURGE_ACCOUNT_REQUEST)
    }

    private fun closeOrOpenAccount() {
        if(activity == null) {
            return
        }
        /*val a: Account = db.getAccount(selectedId)
        if (a.isActive) {
            AlertDialog.Builder(activity!!)
                    .setMessage(R.string.close_account_confirm)
                    .setPositiveButton(R.string.yes) { arg0, arg1 -> flipAccountActive(a) }
                    .setNegativeButton(R.string.no, null)
                    .show()
        } else {
            flipAccountActive(a)
        }*/
    }

    private fun flipAccountActive(a: Account) {
        a.isActive = !a.isActive
        /*db.saveAccount(a)
        recreateCursor()*/
    }

    private fun deleteAccount() {
       /* AlertDialog.Builder(this)
                .setMessage(R.string.delete_account_confirm)
                .setPositiveButton(R.string.yes) { arg0, arg1 ->
                    db.deleteAccount(selectedId)
                    recreateCursor()
                }
                .setNegativeButton(R.string.no, null)
                .show()*/
    }

    private fun integrityCheck() {
        if(activity == null) {
            return
        }
        IntegrityCheckTask(activity!!).execute(IntegrityCheckAutobackup(activity!!, TimeUnit.DAYS.toMillis(7)))
    }
}