package com.handydev.main.googledrive

import android.accounts.Account
import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.handydev.financier.R
import com.handydev.financier.backup.DatabaseExport
import com.handydev.financier.backup.DatabaseImport
import com.handydev.financier.db.DatabaseAdapter
import com.handydev.financier.db.DatabaseAdapter_
import com.handydev.financier.export.ImportExportException
import com.handydev.financier.export.drive.*
import com.handydev.financier.utils.MyPreferences
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.ByteArrayInputStream
import java.io.File

class GoogleDriveClient(var context: Context) {
    var account: Account? = null
    private var driveService: Drive? = null
    private var appFolderId: String? = null

    var db: DatabaseAdapter = DatabaseAdapter_.getInstance_(context)

    init {
        EventBus.getDefault().register(this)
    }

    private fun connect() : Boolean {
        if (driveService == null) {
            val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(
                            DriveScopes.DRIVE_FILE,
                            DriveScopes.DRIVE_APPDATA
                    )
            )
            credential.selectedAccount = account
            driveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory(),
                    credential
            ).setApplicationName("Financier")
                    .build()
            if(driveService == null) {
                return false
            }
            appFolderId = driveService!!.fetchOrCreateAppFolder(getDriveFolderName())
        }
        return driveService != null;
    }

    private fun getDriveFolderName(): String {
        val folder = MyPreferences.getBackupFolder(context)
        if(!(folder != null && folder.isNotEmpty())) {
            throw ImportExportException(R.string.gdocs_folder_not_configured)
        }
        return folder
    }


    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun doBackup(event: DoDriveBackup) {
        val export = DatabaseExport(context, db.db(), true)
        try {
            if(connect()) {
                val fileName = export.generateFilename()
                val bytes = export.generateBackupBytes()
                val status = createFile(fileName, bytes);
                if (status) {
                    handleSuccess(fileName);
                } else {
                    handleFailure(status);
                }
            } else {
                handleFailure(false)
            }
        } catch(_: Exception) {
            handleFailure(false)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun listFiles(event: DoDriveListFiles) {
        try {
            if(connect()) {
                val list = driveService?.getAllFiles()
                if(list != null) {
                    handleSuccess(list)
                } else {
                    handleFailure(false)
                }
            } else {
                handleFailure(false)
            }
        } catch(_: Exception) {
            handleFailure(false)
        }
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun doRestore(event: DoDriveRestore) {
        try {
            if (connect()) {
                val fileContents = driveService?.readFile(event.file.id)
                if(fileContents != null) {
                    try {
                        val inputStream = ByteArrayInputStream(fileContents)
                        DatabaseImport(context, db, inputStream).importDatabase()
                        EventBus.getDefault().post(DriveRestoreSuccess())
                    } finally {
                    }
                } else {
                    handleFailure(false)
                }
            } else {
                handleFailure(false)
            }
        } catch(_: Exception) {
            handleFailure(false)
        }
    }


    fun createFile(name: String, contents: ByteArray): Boolean {
        if(driveService == null || appFolderId == null) {
            return false
        }
        val fileId = driveService?.createFile(appFolderId!!, name)
        if(fileId == null) {
            return false
        }
        driveService!!.saveFile(fileId, name, contents)
        return true
    }

    fun handleFailure(status: Boolean) {
        EventBus.getDefault().post(DriveBackupFailure());
    }

    fun handleSuccess(fileName: String) {
        EventBus.getDefault().post(DriveBackupSuccess(fileName));
    }

    fun handleSuccess(files: FileList) {
        EventBus.getDefault().post(DriveFileList(files))
    }


    fun uploadFile(file: File) {

    }
}