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
import android.content.SharedPreferences
import android.content.pm.PackageInfo
import androidx.core.content.edit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.AppDao
import com.madness.collision.unit.api_viewing.database.AppRoom
import com.madness.collision.util.P
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

data class AppPkgChanges(
    val previousRecords: List<ApiViewingApp>?,
    val changedPackages: List<PackageInfo>
)

interface UpdateRepository {
    fun getPkgChangedTime(): Long
    fun getChangedPackages(context: Context, isFreshInstall: Boolean): Pair<AppPkgChanges, Long>
    fun getMaintainedApp(): ApiViewingApp
}

internal object UpdateRepo {
    fun impl(
        context: Context, appRepo: AppRepository, pkgProvider: PackageInfoProvider): UpdateRepoImpl {
        val dao = AppRoom.getDatabase(context).appDao()
        val mediator = AppMediatorRepo(dao, context.applicationContext)
        val prefs = context.getSharedPreferences(P.PREF_SETTINGS, Context.MODE_PRIVATE)
        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        return UpdateRepoImpl(dao, appRepo, mediator, pkgProvider, prefs, coroutineScope)
    }
}

class UpdateRepoImpl(
    private val appDao: AppDao,
    private val appRepo: AppRepository,
    private val medRepo: AppMediatorRepo,
    private val pkgProvider: PackageInfoProvider,
    private val settingsPrefs: SharedPreferences,
    private val coroutineScope: CoroutineScope,
) : UpdateRepository {
    // timestamp to retrieve app updates, set the first retrieval
    private var lastAppTimestamp: Long = -1L

    override fun getPkgChangedTime(): Long {
        return settingsPrefs.getLong(P.PACKAGE_CHANGED_TIMESTAMP, -1)
    }

    override fun getChangedPackages(context: Context, isFreshInstall: Boolean): Pair<AppPkgChanges, Long> {
        val changes = if (lastAppTimestamp == -1L) {
            val lastTimestamp = when {
                // display recent updates in last week if no history (by default)
                isFreshInstall -> System.currentTimeMillis() - 604_800_000
                else -> settingsPrefs.getLong(P.PACKAGE_CHANGED_TIMESTAMP, -1)
            }
            val currentTime = System.currentTimeMillis()
            // use last persisted timestamp to retrieve updates then persist current time
            val result = detectChanges(pkgProvider.getAll(), lastTimestamp)
            settingsPrefs.edit { putLong(P.PACKAGE_CHANGED_TIMESTAMP, currentTime) }
            lastAppTimestamp = lastTimestamp
            result to currentTime
        } else {
            val currentTime = System.currentTimeMillis()
            // use last the same timestamp used the last time to retrieve updates
            detectChanges(pkgProvider.getAll(), lastAppTimestamp) to currentTime
        }

        // maintain records asynchronously
        coroutineScope.launch {
            appRepo.maintainRecords(context)
        }
        return changes
    }

    private fun detectChanges(allPackages: List<PackageInfo>, timestamp: Long): AppPkgChanges {
        val updateDetector = PackageUpdateDetector { pkgs -> medRepo.get(pkgs, init = false) }
        val updPkgs = updateDetector.getUpdatedPackages(allPackages, timestamp)
        // init = false: ignored properties of past records are not used
        val previous = when (appDao.selectCount()) {
            null -> null
            0 -> emptyList()
            else -> medRepo.get(updPkgs.map { it.packageName }, init = false)
        }
        return AppPkgChanges(previous, updPkgs)
    }

    override fun getMaintainedApp(): ApiViewingApp {
        return appRepo.getMaintainedApp()
    }
}
