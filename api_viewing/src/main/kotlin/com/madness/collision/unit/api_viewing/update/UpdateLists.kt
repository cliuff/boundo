/*
 * Copyright 2022 Clifford Liu
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

package com.madness.collision.unit.api_viewing.update

import android.content.Context
import android.content.pm.PackageInfo
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.madness.collision.unit.api_viewing.MyUpdatesFragment
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.util.hasUsageAccess

object UpdateLists {
    private val usedChecker = UsedPkgChecker()

    class PkgRecords(
        val previousRecords: List<ApiViewingApp>?,
        val changedPackages: List<PackageInfo>,
    )

    class Pkg(
        val records: PkgRecords,
        val lastChangedRecords: Map<String, Long>,
        val hasPkgChanges: Boolean,
    )

    fun getPkgListChanges(context: Context, timestamp: Long, lifecycleOwner: LifecycleOwner,
                          lastChangedRecords: Map<String, Long>): Pkg {
        val (prev, changed) = MyUpdatesFragment.getChangedPackages(context, timestamp, lifecycleOwner)
        val lastChanged = lastChangedRecords
        val pkgListChanged = kotlin.run pkg@{
            if (changed.isEmpty()) return@pkg false
            if (changed.size != lastChanged.size) return@pkg true
            changed.any { it.lastUpdateTime != lastChanged[it.packageName] }
        }
        val newLastChangedRecords = changed.associate { it.packageName to it.lastUpdateTime }
        val lastRecords = lastChanged.map { "${it.key}-${it.value}" }.joinToString()
        val changedRecords = changed.joinToString { "${it.packageName}-${it.lastUpdateTime}" }
        Log.d("AvUpdates", "Checked new update: \n> $lastRecords\n> $changedRecords")
        return Pkg(PkgRecords(prev, changed), newLastChangedRecords, pkgListChanged)
    }

    class Used(
        val usedPkgNames: List<String>,
        val isUsedPkgNamesChanged: Boolean,
        val hasUsageAccess: Boolean,
    )

    fun getUsedListChanges(context: Context, usedPkgNames: List<String>?): Used {
        val hasUsageAccess = context.hasUsageAccess
        val usedPackages = if (hasUsageAccess) usedChecker.get(context) else emptyList()
        val lastUsed = usedPkgNames.orEmpty()
        val usedListChanged = usedPackages != lastUsed
        val usedRecords = usedPackages.joinToString()
        val lastUsedRecords = lastUsed.joinToString()
        Log.d("AvUpdates", "Checked used: \n> $lastUsedRecords\n> $usedRecords")
        return Used(usedPackages, usedListChanged, hasUsageAccess)
    }
}