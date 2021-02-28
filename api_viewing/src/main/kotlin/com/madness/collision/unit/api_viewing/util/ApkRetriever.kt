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
import android.content.pm.PackageInfo
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.util.F
import com.madness.collision.util.file.ContentProviderUtils
import com.madness.collision.util.os.OsUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ApkRetriever(private val context: Context) {
    companion object {
        private const val APP_CACHE_PREFIX = "BoundoApp4Cache"
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

    fun toFile(uri: Uri): File? {
        // identify self owned file
        val cachePath = ContentProviderUtils.resolve(context, uri)
        if (cachePath != null) return File(cachePath)
        // always make copy on Android 10 and above
        if (OsUtils.satisfy(OsUtils.Q)) return makeFileCopy(uri)
        val uriPath = uri.path
        return if (uriPath != null) getRawFile(uriPath) else makeFileCopy(uri)
    }

    private fun getRawFile(path: String): File? {
        val uriPathFile = File(path)
        return if (uriPathFile.exists()) uriPathFile else null
    }

    private fun makeFileCopy(uri: Uri): File? {
        val fileName = "$APP_CACHE_PREFIX${System.currentTimeMillis()}.apk"
        val file = F.createFile(F.cachePublicPath(context), "App", "Apk", fileName)
        if (!F.prepare4(file)) return null
        try {
            val inStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
            inStream.use { FileOutputStream(file).use { outStream -> it.copyTo(outStream) } }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return file
    }

    /**
     * Obtain app from the [info]
     */
    fun resolvePackage(info: PackageInfo, block: (ApiViewingApp?) -> Unit) {
        val ai = info.applicationInfo
        val app = ApiViewingApp(context, info, preloadProcess = true, archive = true)
                .initArchive(context, ai).load(context, ai)
        block.invoke(app)
    }

    /**
     * Obtain app from the [path]
     */
    fun resolvePath(path: String, block: (ApiViewingApp?) -> Unit) {
        val info = MiscApp.getPackageInfo(context, apkPath = path)
        if (info == null) {
            block.invoke(null)
            return
        }
        resolvePackage(info, block)
    }

    suspend fun resolvePath(path: String): ApiViewingApp? {
        return suspendCoroutine { continuation ->
            resolvePath(path) {
                continuation.resume(it)
            }
        }
    }

    /**
     * Obtain apps from the [uri]
     */
    fun resolveUri(uri: Uri, block: (ApiViewingApp) -> Unit) {
        val file = toFile(uri) ?: return
        val nullableAppBlock = nab@ { app: ApiViewingApp? ->
            app ?: return@nab
            block.invoke(app)
        }
        if (file.isDirectory) {
            val paths: MutableList<String> = mutableListOf()
            scanApk(file, paths)
            paths.forEach { resolvePath(it, nullableAppBlock) }
        } else {
            resolvePath(file.path, nullableAppBlock)
        }
    }

    private fun scanApk(folder: File, list: MutableList<String>) {
        if (!folder.exists() || !folder.canRead() || !folder.isDirectory) return
        for (newFile in folder.listFiles() ?: emptyArray()) {
            if (newFile.isDirectory) {
                scanApk(newFile, list)
            } else if (newFile.isFile && newFile.name.endsWith(".apk")) {
                list.add(newFile.path)
            }
        }
    }
}