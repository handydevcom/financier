package com.handydev.financier.activity

import android.Manifest
import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity.RESULT_CANCELED
import android.app.Activity.RESULT_OK
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Contacts.SettingsColumns.KEY
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.handydev.financier.R
import com.handydev.financier.activity.PreferencesActivity.Companion.CHOOSE_ACCOUNT
import com.handydev.financier.activity.PreferencesActivity.Companion.SELECT_DATABASE_FOLDER
import com.handydev.financier.activity.RequestPermission.isRequestingPermission
import com.handydev.financier.app.FinancierApp
import com.handydev.financier.dialog.FolderBrowser
import com.handydev.financier.dialog.PinDialogPreference
import com.handydev.financier.export.Export
import com.handydev.financier.export.drive.DriveBackupError
import com.handydev.financier.export.dropbox.Dropbox
import com.handydev.financier.fragments.FinancierPreferenceDialogFragment
import com.handydev.financier.rates.ExchangeRateProviderFactory
import com.handydev.financier.utils.FingerprintUtils.fingerprintUnavailable
import com.handydev.financier.utils.FingerprintUtils.reasonWhyFingerprintUnavailable
import com.handydev.financier.utils.MyPreferences
import com.handydev.financier.utils.PinProtection
import org.greenrobot.eventbus.EventBus

class PreferencesFragment: PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {
    var pOpenExchangeRatesAppId: Preference? = null
    var dropbox: Dropbox? = null
    private var rootKey: String? = null

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        setGDriveBackupFolder()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val unmaskedRequestCode = requestCode and 0x0000ffff
        if (resultCode == RESULT_OK || resultCode == RESULT_CANCELED) {
            when (unmaskedRequestCode) {
                SELECT_DATABASE_FOLDER -> {
                    val databaseBackupFolder = data?.getStringExtra(FolderBrowser.PATH)
                    MyPreferences.setDatabaseBackupFolder(context, databaseBackupFolder)
                    setCurrentDatabaseBackupFolder()
                }
                CHOOSE_ACCOUNT -> if(data != null) { handleSignInResult(data) }
            }
        }
    }

    private fun handleSignInResult(intent: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(intent)
            .addOnSuccessListener { googleSignInAccount ->
                MyPreferences.setGoogleDriveAccount(context, googleSignInAccount.email)
                FinancierApp.driveClient.account = googleSignInAccount.account
                selectAccount()
            }.addOnFailureListener { e ->
                EventBus.getDefault().post(DriveBackupError(e.message))
            }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        this.rootKey = rootKey
        dropbox = Dropbox(context)
        setPreferencesFromResource(R.xml.preferences, rootKey)
        if(rootKey != null) {
            return
        }
        setGDriveBackupFolder()
        val pLocale: Preference? = preferenceScreen?.findPreference("ui_language")
        pLocale?.setOnPreferenceChangeListener { _, newValue ->
            val locale = newValue as String
            MyPreferences.switchLocale(this@PreferencesFragment.context, locale)
            true
        }
        val pNewTransactionShortcut: Preference? =
            preferenceScreen?.findPreference("shortcut_new_transaction")
        pNewTransactionShortcut?.setOnPreferenceClickListener {
            addShortcut(
                ".activity.TransactionActivity",
                R.string.transaction,
                R.drawable.icon_transaction
            )
            true
        }
        val pNewTransferShortcut: Preference? =
            preferenceScreen.findPreference("shortcut_new_transfer")
        pNewTransferShortcut?.setOnPreferenceClickListener {
            addShortcut(".activity.TransferActivity", R.string.transfer, R.drawable.icon_transfer)
            true
        }
        val pDatabaseBackupFolder: Preference? =
            preferenceScreen.findPreference("database_backup_folder")
        pDatabaseBackupFolder?.setOnPreferenceClickListener {
            if (isRequestingPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                return@setOnPreferenceClickListener false
            }
            selectDatabaseBackupFolder()
            true
        }
        val pGDriveBackupFolder: Preference? = preferenceScreen?.findPreference("backup_folder")
        pGDriveBackupFolder?.setOnPreferenceChangeListener { _, _ ->
            setGDriveBackupFolder()
            true
        }
        val pAuthDropbox: Preference? = preferenceScreen.findPreference("dropbox_authorize")
        pAuthDropbox?.setOnPreferenceClickListener {
            authDropbox()
            true
        }
        val pDeauthDropbox: Preference? = preferenceScreen.findPreference("dropbox_unlink")
        pDeauthDropbox?.setOnPreferenceClickListener {
            deAuthDropbox()
            true
        }
        val pExchangeProvider: Preference? =
            preferenceScreen.findPreference("exchange_rate_provider")
        pOpenExchangeRatesAppId = preferenceScreen.findPreference("openexchangerates_app_id")
        pExchangeProvider?.onPreferenceChangeListener = Preference.OnPreferenceChangeListener() { preference, newValue ->
            pOpenExchangeRatesAppId?.isEnabled = isOpenExchangeRatesProvider(newValue as String?)
            true
        }
        val pDriveAccount: Preference? =
            preferenceScreen.findPreference("google_drive_backup_account")
        pDriveAccount?.setOnPreferenceClickListener { arg0 ->
            chooseAccount()
            true
        }
        val useFingerprint: Preference? =
            preferenceScreen.findPreference("pin_protection_use_fingerprint")
        if (fingerprintUnavailable(context)) {
            useFingerprint?.summary = getString(
                R.string.fingerprint_unavailable,
                reasonWhyFingerprintUnavailable(context)
            )
            useFingerprint?.isEnabled = false
        }
        linkToDropbox()
        setCurrentDatabaseBackupFolder()
        enableOpenExchangeApp()
        selectAccount()
    }

    override fun onDisplayPreferenceDialog(preference: Preference?) {
        if (preference is PinDialogPreference) {
            val fragment = FinancierPreferenceDialogFragment.newInstance(preference.key)
            fragment?.setTargetFragment(this, 0)
            val manager = requireFragmentManager()
            fragment?.show(manager, null)
        } else {
            super.onDisplayPreferenceDialog(preference)
        }
    }

    private fun setGDriveBackupFolder() {
        val pGDriveBackupFolder: Preference? = preferenceScreen?.findPreference("backup_folder")
        pGDriveBackupFolder?.summary = MyPreferences.getBackupFolder(context)
    }

    private fun chooseAccount() {
        val signInOptions: GoogleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE), Scope(DriveScopes.DRIVE_APPDATA))
            .build()
        val client: GoogleSignInClient = GoogleSignIn.getClient(requireContext(), signInOptions)
        client.signOut().addOnSuccessListener {
            MyPreferences.setGoogleDriveAccount(this@PreferencesFragment.context, "")
            startActivityForResult(client.signInIntent, PreferencesActivity.CHOOSE_ACCOUNT)
        }
    }

    private fun selectAccount() {
        val pDriveAccount: Preference? =
            preferenceScreen.findPreference("google_drive_backup_account")
        val account: Account? = selectedAccount
        if (account != null) {
            pDriveAccount?.summary = account.name
        }
    }

    private val selectedAccount: Account?
        get() {
            val accountName = MyPreferences.getGoogleDriveAccount(context)
            if (accountName != null) {
                val accountManager: AccountManager = AccountManager.get(context)
                val accounts: Array<Account> = accountManager.getAccountsByType("com.google")
                for (account in accounts) {
                    if (accountName.equals(account.name)) {
                        return account
                    }
                }
            }
            return null
        }

    private fun linkToDropbox() {
        val dropboxAuthorized: Boolean = MyPreferences.isDropboxAuthorized(context)
        preferenceScreen.findPreference<Preference>("dropbox_unlink")?.isEnabled = dropboxAuthorized
        preferenceScreen.findPreference<Preference>("dropbox_upload_backup")?.isEnabled = dropboxAuthorized
        preferenceScreen.findPreference<Preference>("dropbox_upload_autobackup")?.isEnabled = dropboxAuthorized
    }

    private fun selectDatabaseBackupFolder() {
        val intent = Intent(context, FolderBrowser::class.java)
        intent.putExtra(FolderBrowser.PATH, databaseBackupFolder)
        startActivityForResult(intent, PreferencesActivity.SELECT_DATABASE_FOLDER)
    }

    private fun enableOpenExchangeApp() {
        pOpenExchangeRatesAppId?.isEnabled = MyPreferences.isOpenExchangeRatesProviderSelected(context)
    }

    private val databaseBackupFolder: String
        get() = Export.getBackupFolder(context).absolutePath

    private fun setCurrentDatabaseBackupFolder() {
        val pDatabaseBackupFolder: Preference? =
            preferenceScreen.findPreference("database_backup_folder")
        val summary: String =
            getString(R.string.database_backup_folder_summary, databaseBackupFolder)
        pDatabaseBackupFolder?.summary = summary
    }

    private fun isOpenExchangeRatesProvider(provider: String?): Boolean {
        return ExchangeRateProviderFactory.openexchangerates.name == provider
    }

    private fun addShortcut(activity: String, nameId: Int, iconId: Int) {
        val intent: Intent = createShortcutIntent(
            activity, getString(nameId), Intent.ShortcutIconResource.fromContext(context, iconId),
            "com.android.launcher.action.INSTALL_SHORTCUT"
        )
        requireActivity().sendBroadcast(intent)
    }

    private fun createShortcutIntent(
        activity: String,
        shortcutName: String,
        shortcutIcon: Intent.ShortcutIconResource,
        action: String
    ): Intent {
        val shortcutIntent = Intent()
        shortcutIntent.component = ComponentName(requireContext().packageName, activity)
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val intent = Intent()
        intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
        intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName)
        intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, shortcutIcon)
        intent.action = action
        return intent
    }

    private fun authDropbox() {
        dropbox?.startAuth()
    }

    private fun deAuthDropbox() {
        dropbox?.deAuth()
        linkToDropbox()
    }

    override fun onPause() {
        super.onPause()
        if(rootKey == null) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .unregisterOnSharedPreferenceChangeListener(this)
            PinProtection.lock(context)
        }
    }

    override fun onResume() {
        super.onResume()
        if(rootKey == null) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .registerOnSharedPreferenceChangeListener(this)
            PinProtection.unlock(context)
            dropbox?.completeAuth()
            linkToDropbox()
        }
    }
}

class PreferencesActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartScreenCallback { //PreferenceActivity(), SharedPreferences.OnSharedPreferenceChangeListener {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(MyPreferences.switchLocale(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        val key = intent.getStringExtra(STARTING_KEY)
        val args = Bundle()
        val fragment = PreferencesFragment()
        if(key != null) {
            args.putString(PreferenceFragmentCompat.ARG_PREFERENCE_ROOT, key)
            fragment.arguments = args
        }
        supportFragmentManager.beginTransaction().replace(android.R.id.content, fragment).commit()
    }

    companion object {
        const val SELECT_DATABASE_FOLDER = 100
        const val CHOOSE_ACCOUNT = 101
        const val STARTING_KEY = "Key"
    }

    override fun onPreferenceStartScreen(
        caller: PreferenceFragmentCompat?,
        pref: PreferenceScreen?
    ): Boolean {
        val intent = Intent(this@PreferencesActivity, PreferencesActivity::class.java)
        intent.putExtra(STARTING_KEY, pref?.key)
        startActivity(intent)
        return true
    }
}