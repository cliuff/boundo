package com.madness.collision.wearable.util

import android.content.Context
import android.os.Environment
import java.io.File

/**
File util class
@author Clifford Liu
 **/

object F {

    /**
     * the root of the primary shared storage
     */
    fun externalRoot() = Environment.getExternalStorageDirectory()?.path ?: ""

    fun externalRoot(sub: String) = Environment.getExternalStoragePublicDirectory(sub)?.path ?: ""

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
}
