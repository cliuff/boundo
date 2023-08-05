/*
 * Copyright 2023 Clifford Liu
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

package com.madness.collision.unit.api_viewing.info

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.ModuleInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import com.madness.collision.misc.PackageCompat
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.util.ManifestUtil
import com.madness.collision.util.os.OsUtils

fun ApiViewingApp.isOnBackInvokedCallbackEnabled(info: ApplicationInfo, context: Context): Boolean? {
    return getOnBackInvokedCallbackEnabledAttrValue(this, info, context)
}

private fun getOnBackInvokedCallbackEnabledAttrValue(
    app: ApiViewingApp, info: ApplicationInfo, context: Context): Boolean? {
    val src = app.appPackage.basePath
    val attrValue = ManifestUtil.getOnBackInvokedCallbackEnabled(src)
    if (attrValue.isNotEmpty()) Log.d("av.info", "PreBack/${app.packageName}/$attrValue")
    when (attrValue) {
        "-1", "true" -> return true
        "0", "false" -> return false
        "" -> return null
    }
    val resId = attrValue.toIntOrNull()
    if (resId != null) {
        try {
            return context.packageManager.getResourcesForApplication(info).getBoolean(resId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return null
}

// Added in Android 14, hidden API with access denied
private fun ApplicationInfo.isOnBackInvokedCallbackEnabled(): Boolean? {
    if (OsUtils.dissatisfy(OsUtils.U)) return null
    val info = this
    val klass = info::class.java
    try {
        val method = klass.getDeclaredMethod("isOnBackInvokedCallbackEnabled")
            .apply { isAccessible = true }
        return method.invoke(info) as? Boolean
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

@SuppressLint("PrivateApi")
private fun PackageInfo.isCoreApp(): Boolean? {
    val pkg = this
    return try {
        val field = PackageInfo::class.java.getDeclaredField("coreApp")
            .apply { isAccessible = true }
        field.get(pkg) as? Boolean
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun getPkgSystemModules(context: Context): List<ModuleInfo> {
    if (OsUtils.dissatisfy(OsUtils.Q)) return emptyList()
    val modules = context.packageManager.getInstalledModules(0)
    val pkgNames = PackageCompat.getAllPackages(context.packageManager)
        .mapTo(HashSet()) { it.packageName }
    return modules.filter { it.packageName in pkgNames }
}

private fun getSystemModule(pkgName: String, context: Context): ModuleInfo? {
    if (OsUtils.dissatisfy(OsUtils.Q)) return null
    try {
        return context.packageManager.getModuleInfo(pkgName, 0)
    } catch (_: PackageManager.NameNotFoundException) {
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

@SuppressLint("PrivateApi")
private fun PackageInfo.getOverlayTarget(): Result<String?> {
    val pkg = this
    return try {
        val field = PackageInfo::class.java.getDeclaredField("overlayTarget")
            .apply { isAccessible = true }
        val result = field.get(pkg)
        if (result is String?) Result.success(result) else Result.failure(Exception())
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}

@SuppressLint("PrivateApi")
private fun ApplicationInfo.isInstantApp(): Result<Boolean> {
    val pkg = this
    return try {
        val field = ApplicationInfo::class.java.getDeclaredMethod("isInstantApp")
            .apply { isAccessible = true }
        val result = field.invoke(pkg)
        if (result is Boolean) Result.success(result) else Result.failure(Exception())
    } catch (e: Exception) {
        e.printStackTrace()
        Result.failure(e)
    }
}

private fun ApiViewingApp.isWebApk(): Boolean {
    val prefix = """org.chromium.webapk"""
    return packageName.startsWith(prefix)
}

sealed interface AppType {
    object Common : AppType
    class Overlay(val target: String) : AppType
    object InstantApp : AppType
    object WebApk : AppType
}

fun ApiViewingApp.getAppType(pkgInfo: PackageInfo): AppType {
    if (isWebApk()) return AppType.WebApk
    pkgInfo.getOverlayTarget().onSuccess { t -> if (t != null) return AppType.Overlay(t) }
    pkgInfo.applicationInfo.isInstantApp().onSuccess { i -> if (i) return AppType.InstantApp }
    return AppType.Common
}

object PkgInfo {
    fun getIsCoreApp(pkgInfo: PackageInfo): Boolean? {
        return pkgInfo.isCoreApp()
    }

    fun getPkgModules(context: Context): List<ModuleInfo> {
        return getPkgSystemModules(context)
    }

    fun getModuleInfo(pkgName: String, context: Context): ModuleInfo? {
        return getSystemModule(pkgName, context)
    }
}
