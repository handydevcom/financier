/*******************************************************************************
 * Copyright (c) 2010 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 *
 *
 * Contributors:
 * Denis Solonenko - initial API and implementation
 */
package ru.orangesoftware.main

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import ru.orangesoftware.financisto.R
import ru.orangesoftware.financisto.bus.GreenRobotBus
import ru.orangesoftware.financisto.bus.RefreshCurrentTab
import ru.orangesoftware.financisto.bus.SwitchToMenuTabEvent
import ru.orangesoftware.financisto.db.DatabaseAdapter
import ru.orangesoftware.financisto.db.DatabaseHelper
import ru.orangesoftware.financisto.dialog.WebViewDialog
import ru.orangesoftware.financisto.utils.CurrencyCache
import ru.orangesoftware.financisto.utils.MyPreferences
import ru.orangesoftware.financisto.utils.PinProtection
import androidx.lifecycle.ViewModelProvider
import ru.orangesoftware.financisto.databinding.MainBinding

class MainActivity : AppCompatActivity() {
    private var greenRobotBus: GreenRobotBus? = null
    private lateinit var viewModel: MainViewModel

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(MyPreferences.switchLocale(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        viewModel = ViewModelProvider(this, MainViewModelFactory(supportFragmentManager, this)).get(MainViewModel::class.java)
        val binding: MainBinding =  DataBindingUtil.setContentView(this, R.layout.main)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        //viewModel = MainViewModel(this, supportFragmentManager)
        greenRobotBus = GreenRobotBus()
        initialLoad()

        /*final TabHost tabHost = getTabHost();

        setupAccountsTab(tabHost);
        setupBlotterTab(tabHost);
        setupBudgetsTab(tabHost);
        setupReportsTab(tabHost);
        setupMenuTab(tabHost);*/
        val screen = MyPreferences.getStartupScreen(this)
        //tabHost.setCurrentTabByTag(screen.tag);
        //tabHost.setOnTabChangedListener(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSwitchToMenuTab(event: SwitchToMenuTabEvent?) {
        //getTabHost().setCurrentTabByTag("menu");
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshCurrentTab(e: RefreshCurrentTab?) {
        //refreshCurrentTab();
    }

    override fun onResume() {
        super.onResume()
        greenRobotBus!!.register(this)
        PinProtection.unlock(this)
        if (PinProtection.isUnlocked()) {
            WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this)
        }
    }

    override fun onPause() {
        super.onPause()
        greenRobotBus!!.unregister(this)
        PinProtection.lock(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        PinProtection.immediateLock(this)
    }

    private fun initialLoad() {
        val t3: Long
        val t2: Long
        val t1: Long
        val t0 = System.currentTimeMillis()
        val db = DatabaseAdapter(this)
        db.open()
        try {
            val x = db.db()
            x.beginTransaction()
            t1 = System.currentTimeMillis()
            try {
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, 0, "title", getString(R.string.no_category))
                updateFieldInTable(x, DatabaseHelper.CATEGORY_TABLE, -1, "title", getString(R.string.split))
                updateFieldInTable(x, DatabaseHelper.PROJECT_TABLE, 0, "title", getString(R.string.no_project))
                updateFieldInTable(x, DatabaseHelper.LOCATIONS_TABLE, 0, "name", getString(R.string.current_location))
                updateFieldInTable(x, DatabaseHelper.LOCATIONS_TABLE, 0, "title", getString(R.string.current_location))
                x.setTransactionSuccessful()
            } finally {
                x.endTransaction()
            }
            t2 = System.currentTimeMillis()
            if (MyPreferences.shouldUpdateHomeCurrency(this)) {
                db.setDefaultHomeCurrency()
            }
            CurrencyCache.initialize(db)
            t3 = System.currentTimeMillis()
            if (MyPreferences.shouldRebuildRunningBalance(this)) {
                db.rebuildRunningBalances()
            }
            if (MyPreferences.shouldUpdateAccountsLastTransactionDate(this)) {
                db.updateAccountsLastTransactionDate()
            }
        } finally {
            db.close()
        }
        val t4 = System.currentTimeMillis()
        Log.d("Financisto", "Load time = " + (t4 - t0) + "ms = " + (t2 - t1) + "ms+" + (t3 - t2) + "ms+" + (t4 - t3) + "ms")
    }

    private fun updateFieldInTable(db: SQLiteDatabase, table: String, id: Long, field: String, value: String) {
        db.execSQL("update $table set $field=? where _id=?", arrayOf<Any>(value, id))
    } /*@Override
    public void onTabChanged(String tabId) {
        Log.d("Financisto", "About to update tab " + tabId);
        long t0 = System.currentTimeMillis();
        refreshCurrentTab();
        long t1 = System.currentTimeMillis();
        Log.d("Financisto", "Tab " + tabId + " updated in " + (t1 - t0) + "ms");
    }

    public void refreshCurrentTab() {
        Activity currentActivity = getLocalActivityManager().getCurrentActivity();
        if (currentActivity instanceof RefreshSupportedActivity) {
            RefreshSupportedActivity activity = (RefreshSupportedActivity) currentActivity;
            activity.recreateCursor();
            activity.integrityCheck();
        }
    }

    private void setupAccountsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("accounts")
                .setIndicator(getString(R.string.accounts), getResources().getDrawable(R.drawable.ic_tab_accounts))
                .setContent(new Intent(this, AccountListActivity.class)));
    }

    private void setupBlotterTab(TabHost tabHost) {
        Intent intent = new Intent(this, BlotterActivity.class);
        intent.putExtra(BlotterActivity.SAVE_FILTER, true);
        intent.putExtra(BlotterActivity.EXTRA_FILTER_ACCOUNTS, true);
        tabHost.addTab(tabHost.newTabSpec("blotter")
                .setIndicator(getString(R.string.blotter), getResources().getDrawable(R.drawable.ic_tab_blotter))
                .setContent(intent));
    }

    private void setupBudgetsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("budgets")
                .setIndicator(getString(R.string.budgets), getResources().getDrawable(R.drawable.ic_tab_budgets))
                .setContent(new Intent(this, BudgetListActivity.class)));
    }

    private void setupReportsTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("reports")
                .setIndicator(getString(R.string.reports), getResources().getDrawable(R.drawable.ic_tab_reports))
                .setContent(new Intent(this, ReportsListActivity.class)));
    }

    private void setupMenuTab(TabHost tabHost) {
        tabHost.addTab(tabHost.newTabSpec("menu")
                .setIndicator(getString(R.string.menu), getResources().getDrawable(R.drawable.ic_tab_menu))
                .setContent(new Intent(this, MenuListActivity_.class)));
    }*/
}