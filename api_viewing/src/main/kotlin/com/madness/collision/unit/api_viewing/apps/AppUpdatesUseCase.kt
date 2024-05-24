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

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.madness.collision.util.P
import com.madness.collision.util.hasUsageAccess

class AppUpdatesSession {
    var newAppTimestamp: Long = 0L
    /** Whether showing last week history */
    val isNewApp: Boolean get() = newAppTimestamp > 0L
    var isBrandNewSession = false

    // timestamp to retrieve app updates, set the first retrieval
    var appTimestamp: Long = 0L
    // in accordance to mainTimestamp, set the first retrieval
    var sessionTimestamp: Long = 0L
    // the latest retrieval, set per retrieval
    var lastRetrievalTime: Long = 0L
    // the second to last retrieval, set per retrieval
    var secondLastRetrievalTime: Long = 0L

    fun isNewSession(mainTimestamp: Long): Boolean {
        return sessionTimestamp == 0L || sessionTimestamp != mainTimestamp
    }
}

object AppUpdatesLists {
    val updatesSession = AppUpdatesSession()

    class Pkg(
        val records: AppPkgChanges,
        // the latest change records (until now), we only need "package name" and "update time"
        // (to compare with the time of new changes, thus determine whether to update views)
        val lastChangedRecords: Map<String, Long>,
        val hasPkgChanges: Boolean,
    )

    class Used(
        val usedPkgNames: List<String>,
        val isUsedPkgNamesChanged: Boolean,
        val hasUsageAccess: Boolean,
    )
}

class AppUpdatesUseCase(private val appRepo: AppRepository, val updatesSession: AppUpdatesSession) {
    private val usedChecker = UsedPkgChecker()

    fun getPkgListChanges(context: Context, timestamp: Long, lastChangedRecords: Map<String, Long>): AppUpdatesLists.Pkg {
        val pkgChanges = getChangedPackages(context, timestamp)
        val (lastChanged, changed) = lastChangedRecords to pkgChanges.changedPackages
        val pkgListChanged = kotlin.run pkg@{
            if (changed.isEmpty()) return@pkg false
            if (changed.size != lastChanged.size) return@pkg true
            changed.any { it.lastUpdateTime != lastChanged[it.packageName] }
        }
        val newLastChangedRecords = changed.associate { it.packageName to it.lastUpdateTime }
        val lastRecords = lastChanged.map { "${it.key}-${it.value}" }.joinToString()
        val changedRecords = changed.joinToString { "${it.packageName}-${it.lastUpdateTime}" }
        Log.d("AvUpdates", "Checked new update: \n> $lastRecords\n> $changedRecords")
        return AppUpdatesLists.Pkg(pkgChanges, newLastChangedRecords, pkgListChanged)
    }

    fun getUsedListChanges(context: Context, usedPkgNames: List<String>?): AppUpdatesLists.Used {
        val hasUsageAccess = context.hasUsageAccess
        val usedPackages = if (hasUsageAccess) usedChecker.get(context) else emptyList()
        val lastUsed = usedPkgNames.orEmpty()
        val usedListChanged = usedPackages != lastUsed
        val usedRecords = usedPackages.joinToString()
        val lastUsedRecords = lastUsed.joinToString()
        Log.d("AvUpdates", "Checked used: \n> $lastUsedRecords\n> $usedRecords")
        return AppUpdatesLists.Used(usedPackages, usedListChanged, hasUsageAccess)
    }

    private fun getChangedPackages(context: Context, mainTimestamp: Long): AppPkgChanges = updatesSession.run {
        val prefSettings = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        var lastTimestamp = prefSettings.getLong(P.PACKAGE_CHANGED_TIMESTAMP, -1)
        when {
            // app opened the first time in lifetime, keep this new-app session untouched until reopened
            lastTimestamp == -1L -> newAppTimestamp = System.currentTimeMillis()
            // app reopened, which signals new-app session ended
            mainTimestamp > newAppTimestamp -> newAppTimestamp = 0L
        }
        // display recent updates in last week if no history (by default)
        if (isNewApp) lastTimestamp = System.currentTimeMillis() - 604_800_000

        val isValidSession = lastTimestamp < mainTimestamp
        isBrandNewSession = isNewSession(mainTimestamp) && isValidSession
        val currentTime = System.currentTimeMillis()
        secondLastRetrievalTime = lastRetrievalTime
        lastRetrievalTime = currentTime

        return if (isBrandNewSession) {
            // use last persisted timestamp to retrieve updates then persist current time
            val result = appRepo.getChangedPackages(context, lastTimestamp)
            prefSettings.edit { putLong(P.PACKAGE_CHANGED_TIMESTAMP, currentTime) }
            appTimestamp = lastTimestamp
            sessionTimestamp = mainTimestamp
            result
        } else {
            // use last the same timestamp used the last time to retrieve updates
            appRepo.getChangedPackages(context, appTimestamp)
        }
    }
}
