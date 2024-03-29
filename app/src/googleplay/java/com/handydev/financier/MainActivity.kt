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
package com.handydev.financier

import android.app.Activity
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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.handydev.financier.activity.PreferencesActivity.Companion.CHOOSE_ACCOUNT
import com.handydev.financier.activity.RefreshSupportedActivity
import com.handydev.financier.app.FinancierApp
import com.handydev.financier.base.AbstractListFragment
import com.handydev.financier.bus.RefreshData
import com.handydev.financier.databinding.MainBinding
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.db.DatabaseHelper
import com.handydev.financier.dialog.WebViewDialog
import com.handydev.financier.export.drive.DriveBackupError
import com.handydev.financier.protocol.IOnBackPressed
import com.handydev.financier.utils.CurrencyCache
import com.handydev.financier.utils.MyPreferences
import com.handydev.financier.utils.PinProtection
import com.handydev.financier.fragments.MenuListFragment
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
        tabPager.offscreenPageLimit = 4
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
            if(fragment.isVisible && fragment is RefreshSupportedActivity) {
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

    // Google Drive Region

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == CHOOSE_ACCOUNT && data != null) {
            handleSignInResult(data)
        } else {
            for(fragment in supportFragmentManager.fragments) {
                if(fragment.isVisible && fragment is MenuListFragment) {
                    fragment.redirectedActivityResult(requestCode, resultCode, data)
                }
            }
        }
    }

    fun googleDriveLogin() {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
                .build()
        val client = GoogleSignIn.getClient(this, signInOptions)
        startActivityForResult(client.signInIntent, CHOOSE_ACCOUNT)
    }

    private fun handleSignInResult(intent: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(intent).addOnSuccessListener { googleSignInAccount: GoogleSignInAccount ->
            MyPreferences.setGoogleDriveAccount(this, googleSignInAccount.email)
            FinancierApp.driveClient.account = googleSignInAccount.account
            EventBus.getDefault().post(MenuListFragment.ResumeDriveAction())
        }.addOnFailureListener { e: Exception -> EventBus.getDefault().post(DriveBackupError(e.message)) }
    }
}