package com.handydev.financier.fragments

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.ListFragment
import com.handydev.financier.R
import com.handydev.financier.activity.MenuListItem
import com.handydev.financier.adapter.SummaryEntityListAdapter
import com.handydev.financier.export.csv.CsvExportOptions
import com.handydev.financier.export.csv.CsvImportOptions
import com.handydev.financier.export.dropbox.DropboxBackupTask
import com.handydev.financier.export.dropbox.DropboxFileList
import com.handydev.financier.export.dropbox.DropboxListFilesTask
import com.handydev.financier.export.dropbox.DropboxRestoreTask
import com.handydev.financier.export.qif.QifExportOptions
import com.handydev.financier.export.qif.QifImportOptions
import com.handydev.financier.service.DailyAutoBackupScheduler
import com.handydev.financier.utils.PinProtection
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MenuListFragment: ListFragment() {
    companion object {
        private const val RESOLVE_CONNECTION_REQUEST_CODE = 1
    }

    var bus: EventBus? = null
    lateinit var menuItems: Array<MenuListItem>
    protected fun init() {
        if(activity == null) {
            return
        }
        bus = EventBus.getDefault()
        menuItems = MenuListItem.values()
        menuItems = menuItems.filter { it != MenuListItem.GOOGLE_DRIVE_BACKUP && it != MenuListItem.GOOGLE_DRIVE_RESTORE }.toTypedArray()
        listAdapter = SummaryEntityListAdapter(requireActivity(), menuItems)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    override fun onListItemClick(l: ListView, v: View, position: Int, id: Long) {
        super.onListItemClick(l, v, position, id)
        if(activity == null) {
            return
        }
        menuItems[position].call(requireActivity())
    }

    fun redirectedActivityResult(requestCode: Int, resultCode: Int, data:Intent?) {
        when(requestCode) {
            2 -> onCsvExportResult(resultCode, data)
            3 -> onQifExportResult(resultCode, data)
            4 -> onCsvImportResult(resultCode, data)
            5 -> onQifImportResult(resultCode, data)
            6 -> onChangePreferences()
            1 -> onConnectionRequest(resultCode)
        }
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

    }*/

    /*
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case  2 :
            {
                MenuListActivity_.this.onCsvExportResult(resultCode, data);
                break;
            }
            case  3 :
            {
                MenuListActivity_.this.onQifExportResult(resultCode, data);
                break;
            }
            case  4 :
            {
                MenuListActivity_.this.onCsvImportResult(resultCode, data);
                break;
            }
            case  5 :
            {
                MenuListActivity_.this.onQifImportResult(resultCode, data);
                break;
            }
            case  6 :
            {
                MenuListActivity_.this.onChangePreferences();
                break;
            }
            case  1 :
            {
                MenuListActivity_.this.onConnectionRequest(resultCode);
                break;
            }
        }
    }
     */

    //@OnActivityResult(MenuListItem.ACTIVITY_CSV_EXPORT)
    private fun onCsvExportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && activity != null) {
            val options = CsvExportOptions.fromIntent(data)
            MenuListItem.doCsvExport(requireActivity(), options)
        }
    }

   // @OnActivityResult(MenuListItem.ACTIVITY_QIF_EXPORT)
   private fun onQifExportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && activity != null) {
            val options = QifExportOptions.fromIntent(data)
            MenuListItem.doQifExport(requireActivity(), options)
        }
    }

    //@OnActivityResult(MenuListItem.ACTIVITY_CSV_IMPORT)
    private fun onCsvImportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && activity != null) {
            val options = CsvImportOptions.fromIntent(data)
            MenuListItem.doCsvImport(requireActivity(), options)
        }
    }

    //@OnActivityResult(MenuListItem.ACTIVITY_QIF_IMPORT)
    private fun onQifImportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && activity != null) {
            val options = QifImportOptions.fromIntent(data)
            MenuListItem.doQifImport(requireActivity(), options)
        }
    }

    //@OnActivityResult(MenuListItem.ACTIVITY_CHANGE_PREFERENCES)
    private fun onChangePreferences() {
        if(activity != null) {
            DailyAutoBackupScheduler.scheduleNextAutoBackup(requireActivity())
        }
    }

    override fun onPause() {
        super.onPause()
        if(activity != null) {
            PinProtection.lock(requireActivity())
        }
        bus!!.unregister(this)
    }

    override fun onResume() {
        super.onResume()
        if(activity != null) {
            PinProtection.unlock(requireActivity())
        }
        bus!!.register(this)
    }

    var progressDialog: ProgressDialog? = null

    private fun dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    // google drive

    //@OnActivityResult(RESOLVE_CONNECTION_REQUEST_CODE)
    private fun onConnectionRequest(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK && activity != null) {
            Toast.makeText(requireActivity(), R.string.google_drive_connection_resolved, Toast.LENGTH_LONG).show()
        }
    }

    // dropbox
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun doImportFromDropbox(event: DropboxFileList) {
        val backupFiles = event.files
        if (backupFiles != null && activity != null) {
            val selectedDropboxFile = arrayOfNulls<String>(1)
            AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.restore_database_online_dropbox)
                    .setPositiveButton(R.string.restore) { _, _ ->
                        if (selectedDropboxFile[0] != null) {
                            val d = ProgressDialog.show(requireActivity(), null, getString(R.string.restore_database_inprogress_dropbox), true)
                            DropboxRestoreTask(requireActivity(), d, selectedDropboxFile[0]).execute()
                        }
                    }
                    .setSingleChoiceItems(backupFiles, -1, DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        if (which >= 0 && which < backupFiles.size) {
                            selectedDropboxFile[0] = backupFiles[which]
                        }
                    })
                    .show()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun doDropboxBackup(e: StartDropboxBackup?) {
        if(activity == null) {
            return
        }
        val d = ProgressDialog.show(requireActivity(), null, this.getString(R.string.backup_database_dropbox_inprogress), true)
        DropboxBackupTask(requireActivity(), d).execute()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun doDropboxRestore(e: StartDropboxRestore?) {
        if(activity == null) {
            return
        }
        val d = ProgressDialog.show(requireActivity(), null, this.getString(R.string.dropbox_loading_files), true)
        DropboxListFilesTask(requireActivity(), d).execute()
    }

    class StartDropboxBackup

    class StartDropboxRestore

    class StartDriveBackup

    class ResumeDriveAction

    class StartDriveRestore


}