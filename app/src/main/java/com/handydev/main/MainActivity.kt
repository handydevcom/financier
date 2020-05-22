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
package com.handydev.main

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.Window
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import com.handydev.financier.R
import com.handydev.financier.databinding.MainBinding
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.db.DatabaseHelper
import com.handydev.financier.dialog.WebViewDialog
import com.handydev.financier.utils.CurrencyCache
import com.handydev.financier.utils.MyPreferences
import com.handydev.financier.utils.PinProtection
import com.handydev.main.protocol.IOnBackPressed
import org.greenrobot.eventbus.EventBus

class MainActivity : FragmentActivity() {
    private var eventBus: EventBus? = null
    private lateinit var viewModel: MainViewModel

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(MyPreferences.switchLocale(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        viewModel = ViewModelProvider(this, MainViewModelFactory(this, this)).get(MainViewModel::class.java)
        val binding: MainBinding =  DataBindingUtil.setContentView(this, R.layout.main)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        eventBus = EventBus.getDefault()
        initialLoad()
        val tabPager = findViewById<ViewPager2>(R.id.mainViewPager)
        tabPager.isUserInputEnabled = false
    }

    override fun onBackPressed() {
        val tabPager = findViewById<ViewPager2>(R.id.mainViewPager)
        if(tabPager != null) {
            val fragment = supportFragmentManager.findFragmentByTag("f" + tabPager.currentItem)
            (fragment as? IOnBackPressed)?.onBackPressed()?.not()?.let {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        PinProtection.unlock(this)
        if (PinProtection.isUnlocked()) {
            WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this)
        }
    }

    override fun onPause() {
        super.onPause()
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
    }
}