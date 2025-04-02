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
import com.madness.collision.unit.api_viewing.apps.UpdateRepository
import com.madness.collision.unit.api_viewing.apps.UsedPkgChecker
import com.madness.collision.unit.api_viewing.data.UpdatedApp
import com.madness.collision.util.hasUsageAccess
import kotlin.math.min

class AppUpdatesChecker(private val updateRepo: UpdateRepository) {
    private val usedChecker = UsedPkgChecker()
    private var isFreshInstall: Boolean? = null
    // the latest retrieval, set per retrieval
    private var lastRetrievalTime: Long = -1L

    suspend fun checkNewUpdate(
        changedLimit: Int, usedLimit: Int, context: Context): Map<AppUpdatesIndex, List<UpdatedApp>> {
        // app opened the first time in lifetime
        val isFreshInstall = isFreshInstall
            ?: (updateRepo.getPkgChangedTime() == -1L).also { isFreshInstall = it }
        val (pkg, time) = updateRepo.getChangedPackages(context, isFreshInstall)
        val secondLastRetrievalTime = lastRetrievalTime
        lastRetrievalTime = time
        val usedPackages = if (context.hasUsageAccess) usedChecker.get(context) else emptyList()

        val sections = sortedMapOf<AppUpdatesIndex, List<UpdatedApp>>(compareBy(AppUpdatesIndex::code))
        // todo check if packages are changed to avoid undesirable triggers, see AppUpdatesUseCase
        if (true) {
            val (previousRecords, mChangedPackages) = pkg
            val noRecords = previousRecords == null
            val mPreviousRecords = previousRecords?.associateBy { it.packageName } ?: emptyMap()
            if (mChangedPackages.isNotEmpty()) {
                val detectNew = isFreshInstall || noRecords
                val compareTime = secondLastRetrievalTime
                val listLimitSize = min(mChangedPackages.size, changedLimit)
                AppUpdatesClassifier(mChangedPackages, mPreviousRecords)
                    .getUpdateLists(context, detectNew, compareTime, listLimitSize)
                    .forEach { (type, list) -> sections[type] = list }
            }
        }

        if (usedPackages.isNotEmpty()) {
            val usedSize = min(usedPackages.size, usedLimit)
            val packages = usedPackages.subList(0, usedSize)
            sections[AppUpdatesIndex.USE] = updateRepo.getPersistentApps(packages)
                .map(UpdatedApp::General)
        }
        val iterator = sections.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.isEmpty()) {
                iterator.remove()
            }
        }
        return sections
    }
}
