package com.handydev.main.fragments

import android.app.Activity
import androidx.appcompat.app.AlertDialog
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
import com.google.api.services.drive.model.File
import com.handydev.financier.R
import com.handydev.financier.activity.MenuListItem
import com.handydev.financier.adapter.SummaryEntityListAdapter
import com.handydev.financier.app.FinancierApp
import com.handydev.financier.export.csv.CsvExportOptions
import com.handydev.financier.export.csv.CsvImportOptions
import com.handydev.financier.export.drive.*
import com.handydev.financier.export.dropbox.DropboxBackupTask
import com.handydev.financier.export.dropbox.DropboxFileList
import com.handydev.financier.export.dropbox.DropboxListFilesTask
import com.handydev.financier.export.dropbox.DropboxRestoreTask
import com.handydev.financier.export.qif.QifExportOptions
import com.handydev.financier.export.qif.QifImportOptions
import com.handydev.financier.service.DailyAutoBackupScheduler
import com.handydev.financier.utils.PinProtection
import com.handydev.financier.MainActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class MenuListFragment: ListFragment() {
    companion object {
        private const val RESOLVE_CONNECTION_REQUEST_CODE = 1
    }

    var bus: EventBus? = null

    private fun init() {
        if(activity == null) {
            return
        }
        bus = EventBus.getDefault()
        listAdapter = SummaryEntityListAdapter(requireActivity(), MenuListItem.values())
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
        MenuListItem.values()[position].call(requireActivity())
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

    // google drive
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun doGoogleDriveBackup(e: StartDriveBackup?) {
        if(activity == null) {
            return
        }
        if(FinancierApp.driveClient.account == null) {
            resumeGoogleDriveBackupAction = true
            resumeGoogleDriveRestoreAction = false
            (activity as? MainActivity)?.googleDriveLogin()
            return
        }

        progressDialog = ProgressDialog.show(requireActivity(), null, getString(R.string.backup_database_gdocs_inprogress), true)
        bus!!.post(DoDriveBackup())
    }

    private var resumeGoogleDriveBackupAction = false
    private var resumeGoogleDriveRestoreAction = false

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun resumeDriveAction(e: ResumeDriveAction?) {
        if(activity == null) {
            return
        }
        if(FinancierApp.driveClient.account == null) {
            resumeGoogleDriveBackupAction = false
            resumeGoogleDriveRestoreAction = false
            return
        }
        if(resumeGoogleDriveBackupAction) {
            bus!!.post(StartDriveBackup())
        } else if(resumeGoogleDriveRestoreAction) {
            bus!!.post(StartDriveRestore())
        }
        resumeGoogleDriveBackupAction = false
        resumeGoogleDriveRestoreAction = false
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun doGoogleDriveRestore(e: StartDriveRestore?) {
        if(activity == null) {
            return
        }
        if(FinancierApp.driveClient.account == null) {
            resumeGoogleDriveBackupAction = false
            resumeGoogleDriveRestoreAction = true
            (activity as? MainActivity)?.googleDriveLogin()
            return
        }
        progressDialog = ProgressDialog.show(requireActivity(), null, getString(R.string.google_drive_loading_files), true)
        bus!!.post(DoDriveListFiles())
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDriveList(event: DriveFileList) {
        if(activity == null) {
            return
        }
        dismissProgressDialog()
        val files = event.files
        var filteredFiles = ArrayList<File>()
        for (file in files.files) {
            if(file.mimeType == "application/vnd.google-apps.folder") {
                continue
            }
            filteredFiles.add(file)
        }
        val fileNames = getFileNames(filteredFiles)
        var selectedDriveFile : File? = null
        AlertDialog.Builder(requireActivity())
                .setTitle(R.string.restore_database_online_google_drive)
                .setPositiveButton(R.string.restore) { _, _ ->
                    if (selectedDriveFile != null) {
                        progressDialog = ProgressDialog.show(context, null, getString(R.string.google_drive_restore_in_progress), true)
                        bus!!.post(DoDriveRestore(selectedDriveFile))
                    }
                }
                .setSingleChoiceItems(fileNames, -1, DialogInterface.OnClickListener { _: DialogInterface?, which: Int ->
                    if (which >= 0 && which < files.size) {
                        selectedDriveFile = filteredFiles[which]
                    }
                })
                .show()
    }

    private fun getFileNames(files: ArrayList<File>): Array<String?> {
        var res = arrayOfNulls<String>(files.count())
        var added = 0
        for (file in files) {
            res[added] = file.name
            added += 1
        }
        return res
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
                connectionResult.startResolutionForResult(requireActivity(), RESOLVE_CONNECTION_REQUEST_CODE)
            } catch (e: SendIntentException) {
                // Unable to resolve, message user appropriately
                onDriveBackupError(DriveBackupError(e.message))
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.errorCode, requireActivity(), 0).show()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDriveBackupFailed(event: DriveBackupFailure) {
        if(activity == null) {
            return
        }
        dismissProgressDialog()
        onDriveBackupError(DriveBackupError(getString(R.string.gdocs_connection_failed)))
       /* val status = event.status
        if (status.hasResolution()) {
            try {
                status.startResolutionForResult(requireActivity(), RESOLVE_CONNECTION_REQUEST_CODE)
            } catch (e: SendIntentException) {
                // Unable to resolve, message user appropriately
                onDriveBackupError(DriveBackupError(e.message))
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(status.statusCode, requireActivity(), 0).show()
        }*/
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDriveBackupSuccess(event: DriveBackupSuccess) {
        dismissProgressDialog()
        if(activity == null) {
            return
        }
        Toast.makeText(requireActivity(), getString(R.string.google_drive_backup_success, event.fileName), Toast.LENGTH_LONG).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDriveRestoreSuccess(event: DriveRestoreSuccess?) {
        dismissProgressDialog()
        if(activity == null) {
            return
        }
        Toast.makeText(requireActivity(), R.string.restore_database_success, Toast.LENGTH_LONG).show()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDriveBackupError(event: DriveBackupError) {
        dismissProgressDialog()
        if(activity == null) {
            return
        }
        Toast.makeText(requireActivity(), getString(R.string.google_drive_connection_failed, event.message), Toast.LENGTH_LONG).show()
    }

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