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

import android.content.Context
import android.os.Environment
import java.io.File

/**
File util class
@author Clifford Liu
 **/

object F {
    fun valCachePubLog(context: Context) = createPath(cachePublicPath(context), "Log")
    fun valCachePubAv(context: Context) = createPath(cachePublicPath(context), "App")
    fun valCachePubAvApk(context: Context) = createPath(valCachePubAv(context), "APK")
    fun valCachePubAvLogo(context: Context) = createPath(valCachePubAv(context), "Logo")
    fun valCachePubAvSeal(context: Context) = createPath(valCachePubAv(context), "Seal")

    fun valFilePubExterior(context: Context) = createPath(filePublicPath(context), Environment.DIRECTORY_PICTURES, "Exterior")
    fun valFilePubTwPortrait(context: Context) = createPath(valFilePubExterior(context), "twBack.webp")
    fun valFilePubTwPortraitDark(context: Context) = createPath(valFilePubExterior(context), "twBackDark.webp")

    /**
     * used below Android 10
     */
    fun externalRoot(sub: String) = getExternalStoragePublicDirectory(sub).path ?: ""

    @Suppress("deprecation")
    private fun getExternalStoragePublicDirectory(sub: String): File {
        return Environment.getExternalStoragePublicDirectory(sub)
    }

    fun cachePrivateFile(context: Context) = context.cacheDir

    fun cachePrivatePath(context: Context) = cachePrivateFile(context).path

    fun cachePublicFile(context: Context) = context.externalCacheDir

    fun cachePublicPath(context: Context) = cachePublicFile(context).let { if (it == null) "" else it.path }

    fun filePrivateFile(context: Context) = context.filesDir

    fun filePrivatePath(context: Context) = filePrivateFile(context).path

    fun filePublicFile(context: Context) = context.getExternalFilesDir(null)

    fun filePublicPath(context: Context) = filePublicFile(context).let { if (it == null) "" else it.path }

    fun prepare4(file: File): Boolean {
        return if (file.exists()) file.delete()
        else file.parentFile?.let {
            if (it.exists()) true
            else it.mkdirs()
        } ?: false
    }

    fun prepare4(path: String) = prepare4(File(path))

    fun prepareDir(file: File) = if (file.exists()) true else file.mkdirs()

    fun prepareDir(path: String) = prepareDir(File(path))

    fun affix(vararg names: String) = createPath("", *names)

    fun createPath(path: String, vararg names: String) = StringBuilder(path).apply { names.forEach { append(File.separator).append(it) } }.toString()

    fun createFile(path: String, vararg names: String) = File(createPath(path, *names))

    // Checks if a volume containing external storage is available for read and write.
    val isExternalWritable: Boolean
        get() = Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED

    // Checks if a volume containing external storage is available to at least read.
    val isExternalReadable: Boolean
        get() = Environment.getExternalStorageState() in
                setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
}
