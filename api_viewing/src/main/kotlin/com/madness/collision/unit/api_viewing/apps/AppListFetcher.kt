/*
 * Copyright 2024 Clifford Liu
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

package com.madness.collision.unit.api_viewing.apps

import android.Manifest
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.madness.collision.chief.chiefContext
import com.madness.collision.chief.chiefPkgMan
import com.madness.collision.misc.PackageCompat
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.util.PermissionUtils
import com.madness.collision.util.os.OsUtils
import java.io.File
import kotlin.time.measureTimedValue

object AppListPermission {
    @RequiresApi(Build.VERSION_CODES.R)
    const val QueryAllPackages = Manifest.permission.QUERY_ALL_PACKAGES
    /** Chinese exclusive permission defined in《T/TAF 108-2022 移动终端应用软件列表权限实施指南》. */
    const val GetInstalledApps: String = "com.android.permission.GET_INSTALLED_APPS"
    /** Name of the package in which Chinese exclusive [GetInstalledApps] permission is defined. */
    val GetInstalledAppsPkg: String? = runCatching {
        chiefPkgMan.getPermissionInfo(GetInstalledApps, 0)?.packageName }.getOrNull()

    /** @return The permission required to be granted, or null. */
    fun queryAllPackagesOrNull(context: Context): String? {
        val permissions = when {
            GetInstalledAppsPkg != null -> PermissionUtils.check(context, arrayOf(GetInstalledApps))
            OsUtils.satisfy(OsUtils.R) -> PermissionUtils.check(context, arrayOf(QueryAllPackages))
            else -> emptyList()
        }
        return permissions.firstOrNull()
    }
}

enum class AppListFetcherType { Platform, Storage, Memory }
sealed interface AppListResult {
    class PermissionRequired() : AppListResult
}

interface AppListFetcher<out T> {
    fun getRawList(): List<T>
}


class PlatformAppsFetcher(private val context: Context) : AppListFetcher<PackageInfo> {
    // companion object: direct access to singleton object by simply the class name
    companion object : AppListFetcher<PackageInfo> by PlatformAppsFetcher(chiefContext)

    class Session(val includeApex: Boolean, val includeArchived: Boolean)

    private var session: Session? = null

    fun withSession(includeApex: Boolean = false, includeArchived: Boolean = false): PlatformAppsFetcher {
        session = Session(includeApex = includeApex, includeArchived = includeArchived)
        return this
    }

    override fun getRawList(): List<PackageInfo> {
        queryAllPkgsCheck(context)
        val timed = measureTimedValue {
            val ss = session
            var flags = 0L
            if (OsUtils.satisfy(OsUtils.N)) flags = flags or PackageManager.MATCH_DISABLED_COMPONENTS.toLong()
            if (OsUtils.satisfy(OsUtils.Q) && ss?.includeApex == true) flags = flags or PackageManager.MATCH_APEX.toLong()
            if (OsUtils.satisfy(OsUtils.V) && ss?.includeArchived == true) flags = flags or PackageManager.MATCH_ARCHIVED_PACKAGES
            PackageCompat.getAllPackages(context.packageManager, flags).also { session = null }
        }
        Log.d("AppListFetcher", "PlatformAppListFetcher/${timed.value.size}/${timed.duration}")
        return timed.value
    }
}

private fun queryAllPkgsCheck(context: Context): Boolean {
    val permission = AppListPermission.queryAllPackagesOrNull(context)
    if (permission != null) Log.d("AppListFetcher", "$permission not granted")
    return permission == null
}


data class ShellAppResult(val packageName: String, val apkPath: String?)

class ShellAppsFetcher(private val context: Context) : AppListFetcher<ShellAppResult> {
    companion object : AppListFetcher<ShellAppResult> by ShellAppsFetcher(chiefContext)

    override fun getRawList(): List<ShellAppResult> {
        queryAllPkgsCheck(context)
        val timed = measureTimedValue {
            kotlin.runCatching { retrieveAppList() }
                .onFailure(Throwable::printStackTrace)
                .getOrNull().orEmpty()
        }
        Log.d("AppListFetcher", "ShellAppListFetcher/${timed.value.size}/${timed.duration}")
        return timed.value
    }

    private fun retrieveAppList(): List<ShellAppResult> = buildList {
        // --apex-only not working on Galaxy Tab S4, Android 10
        if (OsUtils.satisfy(OsUtils.Q)) {
            // start processes first before handling outputs to run them concurrently
            val p0 = ProcessBuilder("pm", "list", "packages", "-f").start()
            val p1 = ProcessBuilder("pm", "list", "packages", "-f", "--apex-only").start()
            p0.resolve(::addAll); p1.resolve(::addAll)
        } else {
            ProcessBuilder("pm", "list", "packages", "-f").start().resolve(::addAll)
        }
    }

    private inline fun Process.resolve(outputBlock: (Sequence<ShellAppResult>) -> Unit) {
        inputStream.bufferedReader().useLines { stringSeq ->
            stringSeq.mapNotNull(::parseShellPackage)
                .map { (path, pkg) -> ShellAppResult(pkg, path) }.let(outputBlock)
        }
        errorStream.bufferedReader()
            .useLines { seq -> seq.forEach { Log.e("ShellAppListFetcher", it) } }
    }

    // package:com.madness.collision
    // package:/data/app/~~r81YwItuzCKDhQrgh7OJsA==/com.madness.collision-QII83-vP4Bna87T27lb48Q==/base.apk=com.madness.collision
    private fun parseShellPackage(pkgString: String): Pair<String?, String>? {
        if (pkgString.startsWith("package:").not()) return null
        return when (val sepIndex = pkgString.lastIndexOf('=')) {
            -1 -> null to pkgString.substring(8)
            else -> {
                val path = pkgString.substring(8, sepIndex)
                val pkgName = pkgString.substring(sepIndex + 1)
                (path to pkgName).takeUnless { pkgName.contains(File.separatorChar) }
            }
        }
    }
}


class StorageAppsFetcher(private val context: Context) : AppListFetcher<ApiViewingApp> {
    companion object : AppListFetcher<ApiViewingApp> by StorageAppsFetcher(chiefContext)

    override fun getRawList(): List<ApiViewingApp> {
        return AppRepo.dumb(context).getApps(0)
    }
}