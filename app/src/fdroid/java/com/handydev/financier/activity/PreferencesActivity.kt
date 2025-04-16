package com.handydev.financier.activity

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.ShortcutIconResource
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Bundle
import android.os.PersistableBundle
import android.preference.Preference
import android.preference.PreferenceActivity
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import com.handydev.financier.BuildConfig
import com.handydev.financier.R
import com.handydev.financier.dialog.FolderBrowser
import com.handydev.financier.export.Export
import com.handydev.financier.export.dropbox.Dropbox
import com.handydev.financier.rates.ExchangeRateProviderFactory
import com.handydev.financier.utils.FingerprintUtils
import com.handydev.financier.utils.MyPreferences
import com.handydev.financier.utils.PinProtection
import androidx.preference.PreferenceFragmentCompat

/*class PreferencesActivity : PreferenceActivity(), OnSharedPreferenceChangeListener {
    var pOpenExchangeRatesAppId: Preference? = null
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (BuildConfig.FLAVOR != "fdroid") {
            setGDriveBackupFolder()
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(MyPreferences.switchLocale(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.preferences)
        val preferenceScreen = preferenceScreen
        if (BuildConfig.FLAVOR != "fdroid") {
            setGDriveBackupFolder()
        }
        val pLocale = preferenceScreen.findPreference("ui_language")
        pLocale.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference: Preference?, newValue: Any? ->
                val locale = newValue as String?
                MyPreferences.switchLocale(this@PreferencesActivity, locale)
                true
            }
        val pNewTransactionShortcut = preferenceScreen.findPreference("shortcut_new_transaction")
        pNewTransactionShortcut.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { arg0: Preference? ->
                addShortcut(
                    ".activity.TransactionActivity",
                    R.string.transaction,
                    R.drawable.icon_transaction
                )
                true
            }
        val pNewTransferShortcut = preferenceScreen.findPreference("shortcut_new_transfer")
        pNewTransferShortcut.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { arg0: Preference? ->
                addShortcut(
                    ".activity.TransferActivity",
                    R.string.transfer,
                    R.drawable.icon_transfer
                )
                true
            }
        val pDatabaseBackupFolder = preferenceScreen.findPreference("database_backup_folder")
        pDatabaseBackupFolder.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { arg0: Preference? ->
                if (RequestPermission.isRequestingPermission(
                        this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    )
                ) {
                    false
                // return@setOnPreferenceClickListener false
                } else {
                    selectDatabaseBackupFolder()
                    true
                }
            }
        if (BuildConfig.FLAVOR != "fdroid") {
            val pGDriveBackupFolder = preferenceScreen.findPreference("backup_folder")
            pGDriveBackupFolder.onPreferenceChangeListener =
                Preference.OnPreferenceChangeListener { preference: Preference?, o: Any? ->
                    setGDriveBackupFolder()
                    true
                }
        }
        val pAuthDropbox = preferenceScreen.findPreference("dropbox_authorize")
        pAuthDropbox.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { arg0: Preference? ->
                authDropbox()
                true
            }
        val pDeauthDropbox = preferenceScreen.findPreference("dropbox_unlink")
        pDeauthDropbox.onPreferenceClickListener =
            Preference.OnPreferenceClickListener { arg0: Preference? ->
                deAuthDropbox()
                true
            }
        val pExchangeProvider = preferenceScreen.findPreference("exchange_rate_provider")
        pOpenExchangeRatesAppId = preferenceScreen.findPreference("openexchangerates_app_id")
        pExchangeProvider.onPreferenceChangeListener =
            object : Preference.OnPreferenceChangeListener {
                override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
                    pOpenExchangeRatesAppId?.isEnabled = isOpenExchangeRatesProvider(newValue as String)
                    return true
                }

                private fun isOpenExchangeRatesProvider(provider: String): Boolean {
                    return ExchangeRateProviderFactory.openexchangerates.name == provider
                }
            }
        val useFingerprint = preferenceScreen.findPreference("pin_protection_use_fingerprint")
        if (FingerprintUtils.fingerprintUnavailable(this)) {
            useFingerprint.summary = getString(
                R.string.fingerprint_unavailable,
                FingerprintUtils.reasonWhyFingerprintUnavailable(this)
            )
            useFingerprint.isEnabled = false
        }
        linkToDropbox()
        setCurrentDatabaseBackupFolder()
        enableOpenExchangeApp()
    }

    private fun linkToDropbox() {
        val dropboxAuthorized = MyPreferences.isDropboxAuthorized(this)
        val preferenceScreen = preferenceScreen
        preferenceScreen.findPreference("dropbox_unlink").isEnabled = dropboxAuthorized
        preferenceScreen.findPreference("dropbox_upload_backup").isEnabled = dropboxAuthorized
        preferenceScreen.findPreference("dropbox_upload_autobackup").isEnabled = dropboxAuthorized
    }

    private fun selectDatabaseBackupFolder() {
        val intent = Intent(this, FolderBrowser::class.java)
        intent.putExtra(FolderBrowser.PATH, databaseBackupFolder)
        startActivityForResult(intent, SELECT_DATABASE_FOLDER)
    }

    private fun enableOpenExchangeApp() {
        pOpenExchangeRatesAppId!!.isEnabled =
            MyPreferences.isOpenExchangeRatesProviderSelected(this)
    }

    private val databaseBackupFolder: String
        private get() = Export.getBackupFolder(this).absolutePath

    private fun setCurrentDatabaseBackupFolder() {
        val pDatabaseBackupFolder = preferenceScreen.findPreference("database_backup_folder")
        val summary = getString(R.string.database_backup_folder_summary, databaseBackupFolder)
        pDatabaseBackupFolder.summary = summary
    }

    private fun setGDriveBackupFolder() {
        val pGDriveBackupFolder = preferenceScreen.findPreference("backup_folder")
        pGDriveBackupFolder.summary = MyPreferences.getBackupFolder(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                SELECT_DATABASE_FOLDER -> {
                    val databaseBackupFolder = data.getStringExtra(FolderBrowser.PATH)
                    MyPreferences.setDatabaseBackupFolder(this, databaseBackupFolder)
                    setCurrentDatabaseBackupFolder()
                }
            }
        }
    }

    private fun addShortcut(activity: String, nameId: Int, iconId: Int) {
        val intent = createShortcutIntent(
            activity, getString(nameId), ShortcutIconResource.fromContext(this, iconId),
            "com.android.launcher.action.INSTALL_SHORTCUT"
        )
        sendBroadcast(intent)
    }

    private fun createShortcutIntent(
        activity: String,
        shortcutName: String,
        shortcutIcon: ShortcutIconResource,
        action: String
    ): Intent {
        val shortcutIntent = Intent()
        shortcutIntent.component = ComponentName(this.packageName, activity)
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val intent = Intent()
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName)
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcon)
        intent.action = action
        return intent
    }

    var dropbox = Dropbox(this)
    private fun authDropbox() {
        dropbox.startAuth()
    }

    private fun deAuthDropbox() {
        dropbox.deAuth()
        linkToDropbox()
    }

    override fun onPause() {
        super.onPause()
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        PinProtection.lock(this)
    }

    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
        PinProtection.unlock(this)
        dropbox.completeAuth()
        linkToDropbox()
    }

    companion object {
        private const val SELECT_DATABASE_FOLDER = 100
        const val CHOOSE_ACCOUNT = 101
    }
}*/

class PreferencesActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.preferences_activity)
        val preferenceFragment = PreferenceFragment()
        supportFragmentManager.beginTransaction().replace(R.id.preferences_fragment, preferenceFragment).commit()
    }
}

class PreferenceFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, null)
    }
}