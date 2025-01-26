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
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
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
}

private fun loadByStage(
    pkgInfoProvider: PackageInfoProvider, pkgMgr: PackageManager, labelProvider: AppPkgLabelProvider): Flow<MultiGroupPkgList> {
    return channelFlow {
        val pkgs = pkgInfoProvider.getAll()
        // retrieve launcher pkgs asynchronously
        val deferredLauncherPkgs = async { getLauncherPkgsAsync(pkgs, pkgMgr) }
        // retrieve overlay pkgs asynchronously
        val deferredOverlayPkgs = async {
            pkgs.mapNotNullTo(HashSet()) { p ->
                p.packageName.takeIf { PkgInfo.getOverlayTarget(p) != null }
            }
        }
        val launcherPkgs = deferredLauncherPkgs.await()
        val (launcherPkgList, nonLauncherPkgList) = pkgs.partition { it.packageName in launcherPkgs }
        // avoid retrieving labels asynchronously, which seems to negatively impact launcher labels
        labelProvider.retrieveLabels(launcherPkgList, pkgMgr)
        val deferredNonLauncherLabels = async {
            labelProvider.retrieveLabels(nonLauncherPkgList, pkgMgr)
        }

        // first stage: launcher packages
        val sortedLauncherPkgs = launcherPkgList
            .sortedWith(compareBy(labelProvider.pkgComparator, PackageInfo::packageName))
        send(sortedLauncherPkgs to listOf(launcherPkgs.size))

        val overlayPkgs = deferredOverlayPkgs.await()
        deferredNonLauncherLabels.await()
        val miscPkgs = pkgs.mapNotNullTo(HashSet()) misc@{ p ->
            val pkgName = p.packageName
            if (pkgName in launcherPkgs || pkgName in overlayPkgs) return@misc null
            if ('.' in pkgName) {
                val label = labelProvider.getLabelOrPkg(pkgName)
                if (label == pkgName || label.startsWith("$pkgName.")) return@misc pkgName
            }
            if (p.applicationInfo?.run { icon <= 0 } == true) return@misc pkgName
            null
        }
        val comparator = compareByDescending<PackageInfo> { it.packageName in launcherPkgs }
            .thenBy { it.packageName in overlayPkgs }
            .thenBy { it.packageName in miscPkgs }
            .thenBy { labelProvider.getLabelOrPkg(it.packageName).let { l -> l == it.packageName || l.startsWith("${it.packageName}.") } }
            .thenBy(labelProvider.pkgComparator, PackageInfo::packageName)
        val sortedPkgs = pkgs.sortedWith(comparator)
        val sortedGrouping = listOf(
            launcherPkgs.size, pkgs.size - miscPkgs.size - overlayPkgs.size, pkgs.size - overlayPkgs.size, pkgs.size)

        send(sortedPkgs to sortedGrouping)
    }
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
        pkgMgr.resolveActivity(intent, 0)
    }.getOrNull()
}
