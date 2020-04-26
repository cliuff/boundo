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
    fun valCachePubTt(context: Context) = createPath(cachePublicPath(context), "TT")
    fun valCachePubTtICal(context: Context) = createPath(valCachePubTt(context), "ICal")
    fun valCachePubTtMisc(context: Context) = createPath(valCachePubTt(context), "Misc")
    fun valCachePubTtBridge(context: Context) = createPath(valCachePubTtMisc(context), "bridge.txt")
    fun valCachePubTtUndo(context: Context) = createPath(valCachePubTtICal(context), "Undo")
    fun valCachePubAvApk(context: Context) = createPath(valCachePubAv(context), "APK")
    fun valCachePubAvLogo(context: Context) = createPath(valCachePubAv(context), "Logo")
    fun valCachePubAvSeal(context: Context) = createPath(valCachePubAv(context), "Seal")

    fun valFilePubTt(context: Context) = createPath(filePublicPath(context), "TT")
    fun valFilePubTtCache(context: Context) = createPath(valFilePubTt(context), "Cache")
    fun valFilePubTtICal(context: Context) = createPath(valFilePubTt(context), "ICal")
    fun valFilePubTtHtml(context: Context) = createPath(valFilePubTt(context), "Html")
    fun valFilePubTtCode(context: Context) = createPath(valFilePubTtHtml(context), "Code.html")
    fun valFilePubTtIndicator(context: Context) = createPath(valFilePubTtICal(context), "indicator.ics")
    fun valFilePubTtPrevious(context: Context) = createPath(valFilePubTtICal(context), "previous.ics")
    fun valFilePubTtCurrent(context: Context) = createPath(valFilePubTtICal(context), "current.ics")
    fun valFilePubExterior(context: Context) = createPath(filePublicPath(context), Environment.DIRECTORY_PICTURES, "Exterior")
    fun valFilePubExteriorPortrait(context: Context) = createPath(valFilePubExterior(context), "back.webp")
    fun valFilePubExteriorPortraitDark(context: Context) = createPath(valFilePubExterior(context), "backDark.webp")
    fun valFilePubTwPortrait(context: Context) = createPath(valFilePubExterior(context), "twBack.webp")
    fun valFilePubTwPortraitDark(context: Context) = createPath(valFilePubExterior(context), "twBackDark.webp")

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
