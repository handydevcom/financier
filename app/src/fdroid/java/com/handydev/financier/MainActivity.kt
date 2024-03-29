package com.handydev.financier

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Window
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.handydev.financier.activity.RefreshSupportedActivity
import com.handydev.financier.bus.RefreshData
import com.handydev.financier.databinding.MainBinding
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.db.DatabaseHelper
import com.handydev.financier.dialog.WebViewDialog
import com.handydev.financier.utils.CurrencyCache
import com.handydev.financier.utils.MyPreferences
import com.handydev.financier.utils.PinProtection
import com.handydev.financier.base.AbstractListFragment
import com.handydev.financier.fragments.MenuListFragment
import com.handydev.financier.protocol.IOnBackPressed
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


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
            var handled = false
            if(fragment is IOnBackPressed) {
                handled = fragment.onBackPressed()
            }
            if(!handled) {
                super.onBackPressed()
            }
        } else {
            super.onBackPressed()
        }
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (event?.action == KeyEvent.ACTION_UP) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    viewModel.navigateDirectional(true)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    viewModel.navigateDirectional(false)
                    return true
                }
            }
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val fragment = supportFragmentManager.findFragmentByTag("f" + 4) as? MenuListFragment
        fragment?.redirectedActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() {
        super.onResume()
        PinProtection.unlock(this)
        if (PinProtection.isUnlocked()) {
            WebViewDialog.checkVersionAndShowWhatsNewIfNeeded(this)
        }
        EventBus.getDefault().register(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun doRefresh(data: RefreshData) {
        val tabPager = findViewById<ViewPager2>(R.id.mainViewPager)
        if(tabPager != null) {
            for(i in 0 until tabPager.childCount) {
                val fragment = supportFragmentManager.findFragmentByTag("f$i") as? AbstractListFragment
                if(fragment != null) {
                    fragment.recreateCursor()
                    fragment.integrityCheck()
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        PinProtection.lock(this)
        EventBus.getDefault().unregister(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        PinProtection.immediateLock(this)
    }

    fun refreshCurrentTab() {
        for(fragment in supportFragmentManager.fragments) {
            if(fragment.isVisible && fragment is RefreshSupportedActivity
            ) {
                fragment.recreateCursor()
                fragment.integrityCheck()
            }
        }
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
        Log.d("Financier", "Load time = " + (t4 - t0) + "ms = " + (t2 - t1) + "ms+" + (t3 - t2) + "ms+" + (t4 - t3) + "ms")
    }

    private fun updateFieldInTable(db: SQLiteDatabase, table: String, id: Long, field: String, value: String) {
        db.execSQL("update $table set $field=? where _id=?", arrayOf<Any>(value, id))
    }
}