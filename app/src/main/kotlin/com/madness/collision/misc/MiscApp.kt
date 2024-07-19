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

package com.madness.collision.misc

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log

object MiscApp {
    fun getPackageInfo(context: Context, packageName: String = "", apkPath: String = "",
                       errorMsg: Pair<String, String>? = null): PackageInfo? {
        return when {
            packageName.isNotEmpty() -> {
                try {
                    PackageCompat.getInstalledPackage(context.packageManager, packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    if (errorMsg != null) Log.w(errorMsg.first, errorMsg.second)
                    else e.printStackTrace()
                    null
                }
            }
            apkPath.isNotEmpty() -> getPackageArchiveInfo(context, apkPath)
            else -> null
        }
    }

    fun getApplicationInfo(context: Context, packageName: String = "", apkPath: String = "",
                           errorMsg: Pair<String, String>? = null): ApplicationInfo? {
        return when {
            packageName.isNotEmpty() -> {
                try {
                    PackageCompat.getApplication(context.packageManager, packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    if (errorMsg != null) Log.w(errorMsg.first, errorMsg.second)
                    else e.printStackTrace()
                    null
                }
            }
            apkPath.isNotEmpty() -> getPackageArchiveInfo(context, apkPath)?.applicationInfo
            else -> null
        }
    }

    fun getPackageArchiveInfo(context: Context, path: String): PackageInfo? {
        return PackageCompat.getArchivePackage(context.packageManager, path)?.apply {
            applicationInfo?.sourceDir = path
            applicationInfo?.publicSourceDir = path
        }
    }

    /**
     * Check if an app is installed and enabled
     */
    fun isAppAvailable(context: Context, packageName: String, errorMsg: Pair<String, String>? = null): Boolean {
        val pi = getPackageInfo(context, packageName = packageName, errorMsg = errorMsg) ?: return false
        return pi.applicationInfo?.enabled == true
    }
}
