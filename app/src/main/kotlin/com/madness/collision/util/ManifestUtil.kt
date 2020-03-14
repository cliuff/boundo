package com.madness.collision.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import java.io.File
import java.util.jar.JarFile

object ManifestUtil {
    /**
     * Using [xml][Xml]
     * @param path That of an APK file.
     * @param attr The desired attribute, without name space.
     * @sample getManifestAttr("/sdcard/00a/Boundo.apk", arrayOf("application", "roundIcon"))
     * @see Xml
     */
    fun getManifestAttr(path: String, attr: Array<String> = emptyArray()): String = getManifestAttr(File(path), attr)

    fun getManifestAttr(file: File, attr: Array<String> = emptyArray()): String {
        return try {
            JarFile(file).run {
                getEntry("AndroidManifest.xml")?.let {
                    getInputStream(it)
                }
            }.run {
                if (this == null) return ""
                val by = readBytes()
                close()
                Xml(by, Xml.MODE_FIND, attr).attrAsset
            }/*.run {
                ByteArray(available()).apply { read(this) }
            }*/
            //Tree tr = TrunkFactory.newTree();
            //prt("XML\n"+tr.list());
        } catch (ex: Exception) {
            ex.printStackTrace()
            ""
        }
    }

    fun getIcon(context: Context, applicationInfo: ApplicationInfo, sourceDir: String): Drawable? {
        try {
            val res = context.packageManager.getResourcesForApplication(applicationInfo)
            val resID = getManifestAttr(sourceDir, arrayOf("application", "icon"))
            if (resID.isEmpty()) return null
            return res.getDrawable(resID.toInt(), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getRoundIcon(context: Context, applicationInfo: ApplicationInfo, sourceDir: String): Drawable? {
        try {
            val res = context.packageManager.getResourcesForApplication(applicationInfo)
            val resID = getManifestAttr(sourceDir, arrayOf("application", "roundIcon"))
            if (resID.isEmpty()) return null
            return res.getDrawable(resID.toInt(), null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getMinSdk(sourceDir: String): String {
        try {
            val res = getManifestAttr(sourceDir, arrayOf("uses-sdk", "minSdkVersion"))
            if (res.isEmpty()) return ""
            return res
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
}
