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
import androidx.lifecycle.LifecycleOwner
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.apps.AppRepo
import com.madness.collision.unit.api_viewing.apps.AppUpdatesLists
import com.madness.collision.unit.api_viewing.apps.AppUpdatesUseCase
import com.madness.collision.unit.api_viewing.apps.toPkgApps
import kotlin.math.min

data class AppUpdatesData(val pkg: AppUpdatesLists.Pkg, val used: AppUpdatesLists.Used)

class AppUpdatesChecker {
    private var lastUpdatesData: AppUpdatesData? = null
    private val updatesSession by AppUpdatesLists::updatesSession

    fun isCheckNeeded(): Boolean = lastUpdatesData == null

    fun checkNewUpdate(mainTimestamp: Long, context: Context, lifecycleOwner: LifecycleOwner): Boolean? {
        val appRepo = AppRepo.impl(context.applicationContext, lifecycleOwner)
        val useCase = AppUpdatesUseCase(appRepo, AppUpdatesLists.updatesSession)

        val (lastUpdPkg, lastUpdUsed) = lastUpdatesData?.pkg to lastUpdatesData?.used
        val lastChangedRecords = lastUpdPkg?.lastChangedRecords.orEmpty()
        val pkg = useCase.getPkgListChanges(context, mainTimestamp, lastChangedRecords)

        // recreate from language switching or orientation change (configuration changes)
        val lastUsed = if (updatesSession.isBrandNewSession) null else lastUpdUsed?.usedPkgNames
        val used = useCase.getUsedListChanges(context, lastUsed)
        lastUpdatesData = AppUpdatesData(pkg, used)

        if (used.isUsedPkgNamesChanged) return true
        if (pkg.records.changedPackages.isEmpty()) {
            // always show usage access request
            if (!used.hasUsageAccess) return true
            if (used.usedPkgNames.isEmpty()) return null
        }
        return pkg.hasPkgChanges
    }

    suspend fun getSections(
        changedLimit: Int, usedLimit: Int, context: Context): Map<AppUpdatesIndex, List<*>> {
        val (lastUpdPkg, lastUpdUsed) = lastUpdatesData?.pkg to lastUpdatesData?.used
        val sections = mutableMapOf<AppUpdatesIndex, List<*>>()
        if (lastUpdPkg?.hasPkgChanges == true) {
            val pkgRecords = lastUpdPkg.records
            val mChangedPackages = pkgRecords.changedPackages
            val previousRecords = pkgRecords.previousRecords
            val noRecords = previousRecords == null
            val mPreviousRecords = previousRecords?.associateBy { it.packageName } ?: emptyMap()
            if (mChangedPackages.isNotEmpty()) {
                val detectNew = updatesSession.isNewApp || noRecords
                val compareTime = updatesSession.secondLastRetrievalTime
                val listLimitSize = min(mChangedPackages.size, changedLimit)
                AppUpdatesClassifier(mChangedPackages, mPreviousRecords)
                    .getUpdateLists(context, detectNew, compareTime, listLimitSize)
                    .forEach { (type, list) -> sections[type] = list }
            }
        }

        if (lastUpdUsed?.isUsedPkgNamesChanged == true) {
            val usedPkgList = lastUpdUsed.usedPkgNames
            val usedSize = min(usedPkgList.size, usedLimit)
            val packages = usedPkgList.subList(0, usedSize)
                .mapNotNull { MiscApp.getPackageInfo(context, packageName = it) }
            val anApp = AppRepo.dumb(context).getMaintainedApp()
            sections[AppUpdatesIndex.USE] = packages.toPkgApps(context, anApp)
        }
        return sections
    }
}
