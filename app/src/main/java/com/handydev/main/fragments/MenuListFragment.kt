package com.handydev.main.fragments

import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.ListFragment
import com.google.android.gms.common.GooglePlayServicesUtil
import com.handydev.financier.R
import com.handydev.financier.activity.MenuListItem
import com.handydev.financier.adapter.SummaryEntityListAdapter
import com.handydev.financier.export.csv.CsvExportOptions
import com.handydev.financier.export.csv.CsvImportOptions
import com.handydev.financier.export.drive.*
import com.handydev.financier.export.dropbox.DropboxBackupTask
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

    protected fun init() {
        if(activity == null) {
            return
        }
        bus = EventBus.getDefault()
        listAdapter = SummaryEntityListAdapter(activity!!, MenuListItem.values())
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
        MenuListItem.values()[position].call(activity!!)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            2 -> onCsvExportResult(resultCode, data)
            3 -> onQifExportResult(resultCode, data)
            4 -> onCsvImportResult(resultCode, data)
            5 -> onQifImportResult(resultCode, data)
            6 -> onChangePreferences()
            1 -> onConnectionRequest(resultCode)
        }
    }

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
    fun onCsvExportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && activity != null) {
            val options = CsvExportOptions.fromIntent(data)
            MenuListItem.doCsvExport(activity!!, options)
        }
    }

   // @OnActivityResult(MenuListItem.ACTIVITY_QIF_EXPORT)
    fun onQifExportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && activity != null) {
            val options = QifExportOptions.fromIntent(data)
            MenuListItem.doQifExport(activity!!, options)
        }
    }

    //@OnActivityResult(MenuListItem.ACTIVITY_CSV_IMPORT)
    fun onCsvImportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && activity != null) {
            val options = CsvImportOptions.fromIntent(data)
            MenuListItem.doCsvImport(activity!!, options)
        }
    }

    //@OnActivityResult(MenuListItem.ACTIVITY_QIF_IMPORT)
    fun onQifImportResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && activity != null) {
            val options = QifImportOptions.fromIntent(data)
            MenuListItem.doQifImport(activity!!, options)
        }
    }

    //@OnActivityResult(MenuListItem.ACTIVITY_CHANGE_PREFERENCES)
    fun onChangePreferences() {
        if(activity != null) {
            DailyAutoBackupScheduler.scheduleNextAutoBackup(activity!!)
        }
    }

    override fun onPause() {
        super.onPause()
        if(activity != null) {
            PinProtection.lock(activity!!)
        }
        bus!!.unregister(this)
    }

    override fun onResume() {
        super.onResume()
        if(activity != null) {
            PinProtection.unlock(activity!!)
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

    // google drive
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun doGoogleDriveBackup(e: StartDriveBackup?) {
        if(activity == null) {
            return
        }
        progressDialog = ProgressDialog.show(activity!!, null, getString(R.string.backup_database_gdocs_inprogress), true)
        bus!!.post(DoDriveBackup())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun doGoogleDriveRestore(e: StartDriveRestore?) {
        if(activity == null) {
            return
        }
        progressDialog = ProgressDialog.show(activity!!, null, getString(R.string.google_drive_loading_files), true)
        bus!!.post(DoDriveListFiles())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDriveList(event: DriveFileList) {
        if(activity == null) {
            return
        }
        dismissProgressDialog()
        val files = event.files
        val fileNames = getFileNames(files)
        val selectedDriveFile = arrayOfNulls<DriveFileInfo>(1)
        AlertDialog.Builder(activity)
                .setTitle(R.string.restore_database_online_google_drive)
                .setPositiveButton(R.string.restore) { _, _ ->
                    if (selectedDriveFile[0] != null) {
                        progressDialog = ProgressDialog.show(context, null, getString(R.string.google_drive_restore_in_progress), true)
                        bus!!.post(DoDriveRestore(selectedDriveFile[0]))
                    }
                }
                .setSingleChoiceItems(fileNames, -1, DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    if (which >= 0 && which < files.size) {
                        selectedDriveFile[0] = files[which]
                    }
                })
                .show()
    }

    private fun getFileNames(files: List<DriveFileInfo>): Array<String?> {
        val names = arrayOfNulls<String>(files.size)
        for (i in files.indices) {
            names[i] = files[i].title
        }
        return names
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDriveConnectionFailed(event: DriveConnectionFailed) {
        if(activity == null) {
            return
        }
        dismissProgressDialog()
        val connectionResult = event.connectionResult
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(activity!!, RESOLVE_CONNECTION_REQUEST_CODE)
            } catch (e: SendIntentException) {
                // Unable to resolve, message user appropriately
                onDriveBackupError(DriveBackupError(e.message))
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.errorCode, activity!!, 0).show()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDriveBackupFailed(event: DriveBackupFailure) {
        if(activity == null) {
            return
        }
        dismissProgressDialog()
        val status = event.status
        if (status.hasResolution()) {
            try {
                status.startResolutionForResult(activity!!, RESOLVE_CONNECTION_REQUEST_CODE)
            } catch (e: SendIntentException) {
                // Unable to resolve, message user appropriately
                onDriveBackupError(DriveBackupError(e.message))
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(status.statusCode, activity!!, 0).show()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDriveBackupSuccess(event: DriveBackupSuccess) {
        dismissProgressDialog()
        if(activity == null) {
            return
        }
        Toast.makeText(activity!!, getString(R.string.google_drive_backup_success, event.fileName), Toast.LENGTH_LONG).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDriveRestoreSuccess(event: DriveRestoreSuccess?) {
        dismissProgressDialog()
        if(activity == null) {
            return
        }
        Toast.makeText(activity!!, R.string.restore_database_success, Toast.LENGTH_LONG).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDriveBackupError(event: DriveBackupError) {
        dismissProgressDialog()
        if(activity == null) {
            return
        }
        Toast.makeText(activity!!, getString(R.string.google_drive_connection_failed, event.message), Toast.LENGTH_LONG).show()
    }

    //@OnActivityResult(RESOLVE_CONNECTION_REQUEST_CODE)
    fun onConnectionRequest(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK && activity != null) {
            Toast.makeText(activity!!, R.string.google_drive_connection_resolved, Toast.LENGTH_LONG).show()
        }
    }

    // dropbox
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun doImportFromDropbox(event: DropboxFileList) {
        val backupFiles = event.files
        if (backupFiles != null && activity != null) {
            val selectedDropboxFile = arrayOfNulls<String>(1)
            AlertDialog.Builder(activity!!)
                    .setTitle(R.string.restore_database_online_dropbox)
                    .setPositiveButton(R.string.restore) { _, _ ->
                        if (selectedDropboxFile[0] != null) {
                            val d = ProgressDialog.show(activity!!, null, getString(R.string.restore_database_inprogress_dropbox), true)
                            DropboxRestoreTask(activity!!, d, selectedDropboxFile[0]).execute()
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
        val d = ProgressDialog.show(activity!!, null, this.getString(R.string.backup_database_dropbox_inprogress), true)
        DropboxBackupTask(activity!!, d).execute()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun doDropboxRestore(e: StartDropboxRestore?) {
        if(activity == null) {
            return
        }
        val d = ProgressDialog.show(activity!!, null, this.getString(R.string.dropbox_loading_files), true)
        DropboxListFilesTask(activity!!, d).execute()
    }

    class StartDropboxBackup

    class StartDropboxRestore

    class StartDriveBackup

    class StartDriveRestore


}