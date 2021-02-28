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

package com.madness.collision.util.file

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.madness.collision.BuildConfig
import com.madness.collision.util.F
import com.madness.collision.util.os.OsUtils
import java.io.File

object ContentProviderUtils {
    const val AUTHORITY = "${BuildConfig.APPLICATION_ID}.fileProvider"

    fun getUri(context: Context, file: File): Uri {
        return if (OsUtils.satisfy(OsUtils.N)) {
            FileProvider.getUriForFile(context, AUTHORITY, file)
        } else {
            Uri.fromFile(file)
        }
    }

    fun isMine(uri: Uri): Boolean {
        return uri.authority == AUTHORITY
    }

    /**
     * See file_paths.xml
     */
    fun resolve(context: Context, uri: Uri): String? {
        if (!isMine(uri)) return null
        val path = uri.path ?: return null
        val matchResult = "/(.*?)(/.*)".toRegex().find(path) ?: return null
        val (name, affix) = matchResult.destructured
        return when (name) {
            "privateCache" -> F.cachePrivatePath(context)
            "privateFiles" -> F.filePrivatePath(context)
            "publicCache" -> F.cachePublicPath(context)
            "publicFiles" -> F.filePublicPath(context)
//            "externalStorage" -> F.externalRoot(context)
//            "externalMedia" -> F.externalRoot(context)
            else -> return null
        } + affix
    }
}
