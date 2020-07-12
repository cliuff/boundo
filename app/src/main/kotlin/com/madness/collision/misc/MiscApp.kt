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

package com.madness.collision.misc

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager

object MiscApp {
    fun getPackageInfo(context: Context, packageName: String = "", apkPath: String = ""): PackageInfo?{
        val isArchive = packageName.isEmpty()
        val pm: PackageManager = context.packageManager
        return if (isArchive){
            if (apkPath.isEmpty()) return null
            pm.getPackageArchiveInfo(apkPath, 0)?.apply {
                applicationInfo.sourceDir = apkPath
                applicationInfo.publicSourceDir = apkPath
            }
        }else{
            if (packageName.isEmpty()) return null
            try {
                pm.getPackageInfo(packageName, 0)
            }catch (e: PackageManager.NameNotFoundException){
                e.printStackTrace()
                null
            }
        }
    }

    fun getApplicationInfo(context: Context, packageName: String = "", apkPath: String = ""): ApplicationInfo?{
        val isArchive = packageName.isEmpty()
        val pm: PackageManager = context.packageManager
        return if (isArchive){
            if (apkPath.isEmpty()) return null
            pm.getPackageArchiveInfo(apkPath, 0)?.applicationInfo?.apply {
                sourceDir = apkPath
                publicSourceDir = apkPath
            }
        }else{
            if (packageName.isEmpty()) return null
            try {
                pm.getApplicationInfo(packageName, 0)
            }catch (e: PackageManager.NameNotFoundException){
                e.printStackTrace()
                null
            }
        }
    }
}
