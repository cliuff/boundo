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

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

class PackageUpdateDetector(private val getApps: (List<String>) -> List<ApiViewingApp>) {
    fun getUpdatedPackages(allPackages: List<PackageInfo>, timestamp: Long): List<PackageInfo> {
        val (preinstallPkgs, updatedPkgs) = allPackages
            .groupBy { examPackage(it, timestamp) }
            .run { get(1).orEmpty() to get(2).orEmpty() }
        if (preinstallPkgs.isEmpty()) return updatedPkgs

        val preinstallPkgNames = preinstallPkgs.map { it.packageName }
        val preinstalledRecords = getApps(preinstallPkgNames).associateBy { it.packageName }
        val updPreinstallPkgs = buildList {
            for (info in preinstallPkgs) {
                val record = preinstalledRecords[info.packageName] ?: continue
                if (isDiffPkgWithRecord(info, record)) add(info)
            }
        }
        return listOf(updatedPkgs, updPreinstallPkgs).filterNot { it.isEmpty() }.run {
            when (size) { 0 -> emptyList(); 1 -> get(0); else -> flatten() }
        }
    }

    private fun examPackage(info: PackageInfo, changeTimestamp: Long): Int = when {
        // 0->1970.01.01, 1230768000000->2009.01.01
        info.lastUpdateTime <= 1230768000000L -> 1
        // 1293840000000->2011.01.01, before OS 4.0 was released, to accommodate potential cases
        // with minSDK 23, it is impossible to find an app actually installed by this date
        info.lastUpdateTime <= 1293840000000L -> 1
        // case observed on a Xiaomi device, view as preinstalled case to further examine it
        info.lastUpdateTime < info.firstInstallTime -> 1
        info.lastUpdateTime >= changeTimestamp -> 2
        else -> -1
    }

    private fun isDiffPkgWithRecord(info: PackageInfo, record: ApiViewingApp) = when {
        PackageInfoCompat.getLongVersionCode(info) != record.verCode -> true
        info.versionName != record.verName -> true
        // apex has version in path
        info.applicationInfo.publicSourceDir.orEmpty() != record.appPackage.basePath -> true
        else -> false
    }
}
