/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.handydev.main.googledrive

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.api.services.drive.Drive
import kotlinx.coroutines.coroutineScope

class DriveUploadWorker(
    appContext: Context,
    workerParams: WorkerParameters,
    private val driveService: Drive
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val fileName = inputData.getString(KEY_NAME_ARG)!!
        val contents = inputData.getString(KEY_CONTENTS_ARG)!!
        val folderId = inputData.getString(KEY_CONTENTS_FOLDER_ID)!!
        return coroutineScope {
            val fileId = driveService.createFile(folderId, fileName)
            driveService.saveFile(fileId, fileName, contents)
            Result.success()
        }
    }

    companion object {
        const val KEY_NAME_ARG = "name"
        const val KEY_CONTENTS_ARG = "contents"
        const val KEY_CONTENTS_FOLDER_ID = "folder_id"
    }
}