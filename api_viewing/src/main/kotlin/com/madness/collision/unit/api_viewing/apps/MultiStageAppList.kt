/*
 * Copyright 2025 Clifford Liu
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

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.madness.collision.unit.api_viewing.info.PkgInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/** List is grouped into multiple sections. */
typealias MultiGroupPkgList = Pair<List<PackageInfo>, List<Int>>

object MultiStageAppList {
    fun load(pkgInfoProvider: PackageInfoProvider, pkgMgr: PackageManager, labelProvider: AppPkgLabelProvider) =
        loadByStage(pkgInfoProvider, pkgMgr, labelProvider)

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun loadCats(
        pkgInfoProvider: PackageInfoProvider, pkgMgr: PackageManager, labelProvider: AppPkgLabelProvider): Map<Int, List<PackageInfo>> {

        val pkgs = pkgInfoProvider.getAll()
        val launchers = getLauncherPkgsAsync(pkgs, pkgMgr)
        val (launcherCatApps, userCatServices, _) = getAppsAndServices(pkgs, launchers)
        val launcherPkgList = launcherCatApps.values.toList()
        val userServicePkgList = userCatServices.values.toList()

        labelProvider.retrieveLabels(launcherPkgList, pkgMgr)
        labelProvider.retrieveLabels(userServicePkgList, pkgMgr)
        return (launcherPkgList + userServicePkgList)
            .groupBy { it.applicationInfo?.category ?: ApplicationInfo.CATEGORY_UNDEFINED }
    }
}

private fun loadByStage(
    pkgInfoProvider: PackageInfoProvider, pkgMgr: PackageManager, labelProvider: AppPkgLabelProvider): Flow<MultiGroupPkgList> {
    return channelFlow {
        val pkgs = pkgInfoProvider.getAll()
        // retrieve launcher pkgs asynchronously
        val launchers = getLauncherPkgsAsync(pkgs, pkgMgr)
        val (launcherCatApps, userCatServices, sysCatServices) = getAppsAndServices(pkgs, launchers)
        val launcherPkgs = launcherCatApps.keys
        val userServicePkgs = userCatServices.keys
        val launcherPkgList = launcherCatApps.values.toList()
        val userServicePkgList = userCatServices.values.toList()
        val systemServicePkgList = sysCatServices.values.toList()

        // avoid retrieving labels asynchronously, which seems to negatively impact launcher labels
        labelProvider.retrieveLabels(launcherPkgList, pkgMgr)
        val deferredNonLauncherLabels = async {
            labelProvider.retrieveLabels(userServicePkgList, pkgMgr)
            labelProvider.retrieveLabels(systemServicePkgList, pkgMgr)
        }

        // first stage: launcher packages
        val sortedLauncherPkgs = launcherPkgList
            .sortedWith(compareBy(labelProvider.pkgComparator, PackageInfo::packageName))
        send(sortedLauncherPkgs to listOf(launcherPkgs.size))

        // retrieve overlay pkgs from system services
        val overlayPkgs = systemServicePkgList.mapNotNullTo(HashSet()) { p ->
            p.packageName.takeIf { PkgInfo.getOverlayTarget(p) != null }
        }
        deferredNonLauncherLabels.await()
        val miscPkgs = systemServicePkgList.mapNotNullTo(HashSet()) misc@{ p ->
            val pkgName = p.packageName
            if (pkgName in overlayPkgs) return@misc null
            if ('.' in pkgName) {
                val label = labelProvider.getLabelOrPkg(pkgName)
                if (label == pkgName || label.startsWith("$pkgName.")) return@misc pkgName
            }
            if (p.applicationInfo?.run { icon <= 0 } == true) return@misc pkgName
            null
        }
        val comparator = compareByDescending<PackageInfo> { it.packageName in launcherPkgs }
            .thenByDescending { it.packageName in userServicePkgs }
            .thenBy { it.packageName in overlayPkgs }
            .thenBy { it.packageName in miscPkgs }
            .thenBy { labelProvider.getLabelOrPkg(it.packageName).let { l -> l == it.packageName || l.startsWith("${it.packageName}.") } }
            .thenBy(labelProvider.pkgComparator, PackageInfo::packageName)
        val sortedPkgs = pkgs.sortedWith(comparator)
        val sortedGrouping = listOf(
            launcherPkgs.size,  // launcher
            launcherPkgs.size + userServicePkgs.size,  // user service
            pkgs.size - miscPkgs.size - overlayPkgs.size,  // service
            pkgs.size - overlayPkgs.size,  // misc
            pkgs.size)  // overlay

        send(sortedPkgs to sortedGrouping)
    }
}

private typealias CatApps = Map<String, PackageInfo>

private fun getAppsAndServices(
    pkgs: List<PackageInfo>, launchers: Set<String>): Triple<CatApps, CatApps, CatApps> {

    // launcher apps excluding user debuggable ones
    val launcherApps = mutableMapOf<String, PackageInfo>()
    // user services (non-launcher) & user debuggable apps
    val userServices = mutableMapOf<String, PackageInfo>()
    val systemServices = mutableMapOf<String, PackageInfo>()

    for (p in pkgs) {
        val flags = p.applicationInfo?.flags ?: 0
        val appMap = if (flags and ApplicationInfo.FLAG_SYSTEM != 0) {
            if (p.packageName in launchers) launcherApps else systemServices
        } else if (flags and ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            userServices
        } else {
            if (p.packageName in launchers) launcherApps else userServices
        }
        appMap[p.packageName] = p
    }
    return Triple(launcherApps, userServices, systemServices)
}

// todo cache
// async on every item may have negative impact on performance depending on the device
private suspend fun getLauncherPkgsAsync(pkgs: List<PackageInfo>, pkgMgr: PackageManager): Set<String> {
    if (pkgs.isEmpty()) return emptySet()
    return coroutineScope {
        val launcherPkgs = Array(pkgs.size) { i ->
            async {
                val info = resolveLauncherActivity(pkgs[i].packageName, pkgMgr)
                info?.activityInfo?.packageName
            }
        }
        awaitAll(*launcherPkgs).filterNotNullTo(HashSet())
    }
}

private fun getLauncherPkgs(pkgs: List<PackageInfo>, pkgMgr: PackageManager): Set<String> {
    if (pkgs.isEmpty()) return emptySet()
    return pkgs.mapNotNullTo(HashSet()) { p ->
        val info = resolveLauncherActivity(p.packageName, pkgMgr)
        info?.activityInfo?.packageName
    }
}

private fun resolveLauncherActivity(pkgName: String, pkgMgr: PackageManager): ResolveInfo? {
    return runCatching {
        // getLaunchIntentForPackage looks for Intent.CATEGORY_INFO that we don't need
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setPackage(pkgName)

        // PackageManager.MATCH_DISABLED_COMPONENTS:
        // include launcher activities that are disabled by default,
        // e.g. Android Files, Digital Wellbeing, Gboard, etc.

        // Use queryIntentActivities() instead of resolveActivity(),
        // which returns ResolverActivity for multiple matching activities.
        pkgMgr.queryIntentActivities(intent, 0).firstOrNull()
    }.getOrNull()
}
