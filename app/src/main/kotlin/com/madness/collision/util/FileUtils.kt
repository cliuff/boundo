/*
 * Copyright 2020 Clifford Liu
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

package com.madness.collision.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import java.io.File
import java.net.MalformedURLException
import java.net.URI

object FileUtils {

    fun getName(context: Context, fileUri: Uri): String {
        val cr = context.contentResolver
        val cursor = cr.query(fileUri, null,
                null, null, null)
        cursor ?: return ""
        cursor.moveToFirst()
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val re = cursor.getString(index)
        cursor.close()
        return re
    }

    fun getSize(context: Context, fileUri: Uri): Long {
        val cr = context.contentResolver
        val cursor = cr.query(fileUri, null,
                null, null, null)
        cursor ?: return 0
        cursor.moveToFirst()
        val index: Int = cursor.getColumnIndex(OpenableColumns.SIZE)
        val re = cursor.getLong(index)
        cursor.close()
        return re
    }

    fun getType(context: Context, fileUri: Uri): String {
        val cr = context.contentResolver
        return cr.getType(fileUri) ?: ""
    }

    @RequiresApi(X.Q)
    fun getTypeInfo(context: Context, fileUri: Uri): ContentResolver.MimeTypeInfo {
        val type = getType(context, fileUri)
        val cr = context.contentResolver
        return cr.getTypeInfo(type)
    }

    fun getMimeType(file: File): String {
        return getMimeType(file.toURI())
    }

    fun getMimeType(uri: URI): String {
        val url = try {
            uri.toURL()
        } catch (e: MalformedURLException) {
            e.printStackTrace()
            return ""
        }
        return getMimeType(url.toString())
    }

    fun getMimeType(url: String): String {
        val extension = MimeTypeMap.getFileExtensionFromUrl(url) ?: return ""
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: ""
    }
}
