package com.handydev.main.googledrive

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

import com.google.api.client.http.ByteArrayContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveRequest
import com.google.api.services.drive.model.File
import com.google.api.services.drive.model.FileList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.http.Consts
import org.apache.http.entity.ContentType

suspend fun <T> DriveRequest<T>.executeWithCoroutines(): T {
    return withContext(Dispatchers.IO) {
        execute()
    }
}

/**
 * Calls the Drive API to see if your app has a folder already, and if not creates one.
 *
 * @return the fetched folderId if existed or the newly created folderId
 */
suspend fun Drive.fetchOrCreateAppFolder(folderName: String): String {
    val folderList = getAppFolder()

    return if (folderList.files.isEmpty()) {
        val fileMetadata = File().apply {
            name = folderName
            mimeType = DriveContentType.DRIVE_FOLDER.mimeType
        }
        files().create(fileMetadata)
                .setFields("id")
                .executeWithCoroutines()
                .id
    } else {
        folderList.getFirst().id
    }
}

/**
 * Creates a text file in the user's Drive, in the application's folder and returns its file ID.
 * Mime type is text files only for this sample, change for your use case
 *
 * @return newly created fileId
 */
suspend fun Drive.createFile(folderId: String, fileName: String): String {
    val metadata = File().apply {
        parents = listOf(folderId)
        mimeType = ContentType.TEXT_PLAIN.mimeType
        name = fileName
    }
    return files()
            .create(metadata)
            .executeWithCoroutines()
            .id
}

/**
 * Opens the file identified by the given [fileId] and returns a [Pair] of its name and contents.
 */
suspend fun Drive.readFile(fileId: String): Pair<String, String> {
    // Retrieve the metadata as a File object.
    val metadata = files().get(fileId).execute()
    val name = metadata.name

    // Stream the file contents to a String.
    return withContext(Dispatchers.IO) {
        val content = files().get(fileId).executeMediaAsInputStream()
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: ""
        name to content
    }
}

/**
 * Updates the file identified by [fileId] with the given [name] and [content].
 */
suspend fun Drive.saveFile(fileId: String, name: String, content: String) {
    // Create a File containing any metadata changes.
    val metadata = File().setName(name)

    val contentStream = ByteArrayContent.fromString(ContentType.TEXT_PLAIN.mimeType, content)

    // Update the metadata and contents.
    files().update(fileId, metadata, contentStream).executeWithCoroutines()
}

/**
 * Returns a [FileList] containing all the visible files in the user's My Drive.
 *
 * The returned list will only contain files visible to this app, i.e. those which were
 * created by this app.
 */
suspend fun Drive.getAllFiles(): FileList {
    return files().list().setSpaces("drive").executeWithCoroutines()
}

/**
 * Returns a [FileList] containing the folder associated with the app.
 *
 * The returned list will only contain folders created by the app, which in best practice should
 * only be 1 unless you need nested folders.
 */
suspend fun Drive.getAppFolder(): FileList {
    return files().list()
            .setSpaces("drive")
            .setQ("mimeType = '${DriveContentType.DRIVE_FOLDER.mimeType}'")//Special Drive folder mimeType
            .executeWithCoroutines()
}

fun FileList.getFirst(): File = files[0]

object DriveContentType {
    val DRIVE_FOLDER = ContentType.create(
            "application/vnd.google-apps.folder",
            Consts.ISO_8859_1
    )
}