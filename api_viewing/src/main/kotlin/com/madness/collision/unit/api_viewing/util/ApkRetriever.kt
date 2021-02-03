/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.api_viewing.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.madness.collision.util.os.OsUtils
import java.io.File

class ApkRetriever(private val context: Context) {
    companion object {
        const val APP_CACHE_PREFIX = "BoundoApp4Cache"
    }

    /**
     * For both file and tree URIs
     */
    fun fromDocumentUri(uri: Uri, block: (Uri) -> Unit) {
        val doc = DocumentFile.fromSingleUri(context, uri)!!
        if (!doc.canRead()) return
        val docUri = doc.uri
        if (doc.isFile) {
            if (doc.type == "application/vnd.android.package-archive"
                    && doc.name?.contains(APP_CACHE_PREFIX) != true) {
                block.invoke(docUri)
            }
            return
        }
        val mTreeUri = if (OsUtils.satisfy(OsUtils.N) && !DocumentsContract.isTreeUri(docUri)) {
            val docId = DocumentsContract.getDocumentId(docUri)
            DocumentsContract.buildTreeDocumentUri(docUri.authority, docId)
        } else {
            docUri
        }
        // todo created tree URI is not accessible, ContentResolver.query(...) throws SecurityException
        fromUri(mTreeUri, block)
    }

    /**
     * Get APK file URI from file/folder URI, for tree URIs only
     */
    fun fromUri(uri: Uri, block: (Uri) -> Unit) {
        val parentDocId = DocumentsContract.getTreeDocumentId(uri)
        fromTreeUri(uri, parentDocId, block)
    }

    private fun fromTreeUri(treeUri: Uri, parentDocId: String, block: (Uri) -> Unit) {
//        if (!DocumentsContract.isDocumentUri(context, treeUri)) return
        val contentResolver = context.contentResolver ?: return
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)

        val columns = arrayOf(
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
        )
        try {
            val childCursor = contentResolver.query(childrenUri, columns, null, null, null)
            childCursor?.use { cursor ->
                while (cursor.moveToNext()) {
                    val mimeType = cursor.getString(0) ?: ""
                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        val id = cursor.getString(1)
                        fromTreeUri(treeUri, id, block)
                        continue
                    }
                    if (mimeType != "application/vnd.android.package-archive") continue
                    val id = cursor.getString(1)
                    val name = cursor.getString(2)
                    if (name.contains(APP_CACHE_PREFIX)) continue
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, id) ?: continue
                    block.invoke(childUri)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun fromFileFolder(folder: File, block: (File) -> Unit) {
        if (!folder.exists() || !folder.canRead() || !folder.isDirectory) return
        try {
            for (newFile in folder.listFiles() ?: emptyArray()) {
                if (newFile.isDirectory) {
                    fromFileFolder(newFile, block)
                    continue
                }
                if (newFile.isFile && newFile.name.endsWith(".apk")) {
                    block.invoke(newFile)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}