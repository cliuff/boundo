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

package com.madness.collision.unit.api_viewing.ui.upd

import android.content.Context
import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import com.madness.collision.unit.api_viewing.apps.AppRepo
import com.madness.collision.unit.api_viewing.apps.toPkgApps
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.UpdatedApp
import com.madness.collision.unit.api_viewing.ui.list.AppApiMode
import com.madness.collision.unit.api_viewing.ui.list.AppListOrder
import com.madness.collision.unit.api_viewing.ui.list.getComparator
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

enum class AppUpdatesIndex(val code: Int) {
    NEW(0),
    UPG(1),
    VER(2),
    PCK(3),
    REC(4),
    USE(10),
}

class AppUpdatesClassifier(
    private val changedPkgList: List<PackageInfo>,
    private val previousRecords: Map<String, ApiViewingApp>) {

    suspend fun getUpdateLists(
        context: Context, detectNew: Boolean, compareUpdateTime: Long, listLimitSize: Int
    ): Map<AppUpdatesIndex, List<UpdatedApp>> = coroutineScope {
        val sections = mutableMapOf<AppUpdatesIndex, List<UpdatedApp>>()
        val anApp = AppRepo.dumb(context).getMaintainedApp()
        val packages = changedPkgList.subList(0, listLimitSize)
        val appList = packages.toPkgApps(context, anApp.clone() as ApiViewingApp)

        val cLists = async {
            val comparator = AppListOrder.UpdateTime.getComparator(AppApiMode.Target)
                .let { comp -> compareBy(comp, UpdatedApp::app) }
            classify(appList, detectNew, compareUpdateTime)
                .map { (type, list) -> async { type to list.map(UpdatedApp::General).sortedWith(comparator) } }
                .awaitAll()
        }
        val upgradeList = async {
            val appSet = appList.associateBy(ApiViewingApp::packageName)
            val comparator = compareBy<UpdatedApp.VersionUpgrade> { it.updateTime.second }.reversed()
            val apiUpgrades = getUpgrades(changedPkgList, appSet, context, anApp.clone() as ApiViewingApp)
            val verUpgrades = getVerUpgrades(changedPkgList, appSet, context, anApp)
            // exclude API upgrades from version upgrades
            val dupPkgs = apiUpgrades.run { mapTo(HashSet(size)) { it.app.packageName } }
            val distinctVerUpgrades = verUpgrades.filterNot { it.app.packageName in dupPkgs }
            apiUpgrades.sortedWith(comparator) to distinctVerUpgrades.sortedWith(comparator)
        }
        cLists.await().forEach { (type, list) -> sections[type] = list }
        val (apiUpgrades, verUpgrades) = upgradeList.await()
        sections[AppUpdatesIndex.UPG] = apiUpgrades
        sections[AppUpdatesIndex.VER] = verUpgrades
        sections
    }

    private fun classify(appList: List<ApiViewingApp>, detectNew: Boolean, compareTime: Long) =
        appList.groupBy gp@{ app ->
            val prevApp = previousRecords[app.packageName]
            if (!detectNew && prevApp == null) return@gp AppUpdatesIndex.NEW
            if (prevApp != null && app.verCode != prevApp.verCode) return@gp AppUpdatesIndex.VER
            val isPkgUpdate = when {
                compareTime <= 0 -> prevApp != null && app.updateTime > prevApp.updateTime
                else -> app.updateTime >= compareTime
            }
            if (isPkgUpdate) AppUpdatesIndex.PCK else AppUpdatesIndex.REC
        }

    private suspend fun getUpgrades(
        changedPkgList: List<PackageInfo>,
        appSet: Map<String, ApiViewingApp>,
        context: Context, anApp: ApiViewingApp
    ): List<UpdatedApp.Upgrade> {
        // upgradePackages: upgrades that can be found in appList
        // getPackages: upgrades that are missing, i.e. excluded from appList
        val (upgradePackages, getPackages) = changedPkgList.asSequence()
            .mapNotNull { p -> previousRecords[p.packageName]?.let { it to p } }
            .partition { (_, p) -> appSet[p.packageName] != null }
        val upgradesA = upgradePackages
            .mapNotNull { (prev, p) -> UpdatedApp.Upgrade.get(prev, appSet[p.packageName]!!) }
        // get missing apps and missing upgrades
        val getApps = getPackages
            .mapNotNull { (prev, p) -> p.takeIf { prev.targetAPI != p.applicationInfo?.targetSdkVersion } }
            .toPkgApps(context, anApp)
            .associateBy(ApiViewingApp::packageName)
        val upgradesB = getPackages
            .mapNotNull { (prev, _) -> getApps[prev.packageName]?.let { UpdatedApp.Upgrade.get(prev, it) } }
        return upgradesA + upgradesB
    }

    private suspend fun getVerUpgrades(
        changedPkgList: List<PackageInfo>,
        appSet: Map<String, ApiViewingApp>,
        context: Context, anApp: ApiViewingApp
    ): List<UpdatedApp.VersionUpgrade> {
        // upgradePackages: upgrades that can be found in appList
        // getPackages: upgrades that are missing, i.e. excluded from appList
        val (upgradePackages, getPackages) = changedPkgList.asSequence()
            .mapNotNull { p -> previousRecords[p.packageName]?.let { it to p } }
            .partition { (_, p) -> appSet[p.packageName] != null }
        val upgradesA = upgradePackages
            .mapNotNull { (prev, p) -> UpdatedApp.VersionUpgrade.get(prev, appSet[p.packageName]!!) }
        // get missing apps and missing upgrades
        val getApps = getPackages
            .mapNotNull { (prev, p) -> p.takeIf { prev.verCode != PackageInfoCompat.getLongVersionCode(p) } }
            .toPkgApps(context, anApp)
            .associateBy(ApiViewingApp::packageName)
        val upgradesB = getPackages
            .mapNotNull { (prev, _) -> getApps[prev.packageName]?.let { UpdatedApp.VersionUpgrade.get(prev, it) } }
        return upgradesA + upgradesB
    }
}
