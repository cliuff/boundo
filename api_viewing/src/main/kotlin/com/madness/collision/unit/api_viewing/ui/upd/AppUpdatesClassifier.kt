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
import androidx.lifecycle.LifecycleOwner
import com.madness.collision.unit.api_viewing.apps.toPkgApps
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.AppMaintainer
import com.madness.collision.unit.api_viewing.ui.list.AppApiMode
import com.madness.collision.unit.api_viewing.ui.list.AppListOrder
import com.madness.collision.unit.api_viewing.ui.list.getComparator
import com.madness.collision.unit.api_viewing.upgrade.Upgrade
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
        context: Context, detectNew: Boolean, compareUpdateTime: Long,
        listLimitSize: Int, lifecycleOwner: LifecycleOwner
    ): Map<AppUpdatesIndex, List<*>> = coroutineScope {
        val sections = mutableMapOf<AppUpdatesIndex, List<*>>()
        val anApp = AppMaintainer.get(context, lifecycleOwner)
        val packages = changedPkgList.subList(0, listLimitSize)
        val appList = packages.toPkgApps(context, anApp.clone() as ApiViewingApp)

        val cLists = async {
            val comparator = AppListOrder.UpdateTime.getComparator(AppApiMode.Target)
            classify(appList, detectNew, compareUpdateTime)
                .map { (type, list) -> async { type to list.sortedWith(comparator) } }
                .awaitAll()
        }
        val upgradeList = async {
            val appSet = appList.associateBy(ApiViewingApp::packageName)
            val comparator = compareBy<Upgrade> { it.updateTime.second }.reversed()
            getUpgrades(changedPkgList, appSet, context, anApp).sortedWith(comparator)
        }
        cLists.await().forEach { (type, list) -> sections[type] = list }
        sections[AppUpdatesIndex.UPG] = upgradeList.await()
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
    ): List<Upgrade> {
        // upgradePackages: upgrades that can be found in appList
        // getPackages: upgrades that are missing, i.e. excluded from appList
        val (upgradePackages, getPackages) = changedPkgList.asSequence()
            .mapNotNull { p -> previousRecords[p.packageName]?.let { it to p } }
            .partition { (_, p) -> appSet[p.packageName] != null }
        val upgradesA = upgradePackages
            .mapNotNull { (prev, p) -> Upgrade.get(prev, appSet[p.packageName]!!) }
        // get missing apps and missing upgrades
        val getApps = getPackages.map { (_, p) -> p }.toPkgApps(context, anApp)
        val upgradesB = getPackages
            .mapIndexedNotNull { i, (prev, _) -> Upgrade.get(prev, getApps[i]) }
        return upgradesA + upgradesB
    }
}
