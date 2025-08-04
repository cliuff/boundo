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
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.chief.auth.PermissionState
import com.madness.collision.unit.api_viewing.apps.AppListPermission
import com.madness.collision.unit.api_viewing.apps.AppRepo
import com.madness.collision.unit.api_viewing.apps.AppRepository
import com.madness.collision.unit.api_viewing.apps.PlatformAppProvider
import com.madness.collision.unit.api_viewing.apps.UpdateRepo
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.UpdatedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.EnumMap

typealias AppUpdatesSections = Map<AppUpdatesIndex, List<UpdatedApp>>

data class AppUpdatesPermState(
    val canQueryAllPkgs: Boolean,
    /** [AppListPermission.QueryAllPackages] or [AppListPermission.GetInstalledApps]. */
    val queryPermission: String?,
    /** Whether we should request runtime permission, or ask the user to change settings. */
    val canReqRuntimePerm: Boolean,
)

data class AppUpdatesUiState(
    val isLoading: Boolean,
    val sections: AppUpdatesSections,
    val perm: AppUpdatesPermState,
)

class AppUpdatesViewModel : ViewModel() {
    private val mutUiState: MutableStateFlow<AppUpdatesUiState> = kotlin.run {
        val perm = AppUpdatesPermState(
            canQueryAllPkgs = true,
            queryPermission = null,
            canReqRuntimePerm = false,
        )
        MutableStateFlow(AppUpdatesUiState(isLoading = true, sections = emptyMap(), perm = perm))
    }
    val uiState: StateFlow<AppUpdatesUiState> = mutUiState.asStateFlow()
    private val mutAppListId = MutableStateFlow(0)
    val appListId: StateFlow<Int> = mutAppListId.asStateFlow()
    private val optionsOwner = AppUpdatesOptionsOwner()
    private var updatesOptions: AppUpdatesOptions? = null
    private var appRepo: AppRepository? = null
    private var updatesChecker: AppUpdatesChecker? = null
    private val mutexUpdatesCheck = Mutex()
    var columnCount: Int = 1
    var sectionsAppList: List<ApiViewingApp> = emptyList()
        private set

    fun setAllPkgsQueryResult(state: PermissionState) {
        mutUiState.update { curr ->
            curr.copy(perm = when (state) {
                PermissionState.Granted ->
                    curr.perm.copy(canQueryAllPkgs = true)
                is PermissionState.ShowRationale ->
                    curr.perm.copy(canQueryAllPkgs = false, canReqRuntimePerm = true)
                is PermissionState.Denied ->
                    curr.perm.copy(canQueryAllPkgs = false, canReqRuntimePerm = true)
                is PermissionState.PermanentlyDenied ->
                    curr.perm.copy(canQueryAllPkgs = false, canReqRuntimePerm = false)
            })
        }
    }

    private var updatesCheckTime: Long = 0L
    private var updatesCheckResumeTime: Long = 0L
    private var refreshTime: Long = 0L

    fun refreshUpdates(context: Context) {
        if (SystemClock.uptimeMillis() - refreshTime > 500) {
            refreshTime = SystemClock.uptimeMillis()
            checkUpdates(context)
        }
    }

    fun checkResumedUpdates(context: Context) {
        if (SystemClock.uptimeMillis() - updatesCheckResumeTime > 30_000) {
            updatesCheckResumeTime = SystemClock.uptimeMillis()
            checkUpdates(context)
        }
    }

    fun checkUpdates(context: Context) {
        if (SystemClock.uptimeMillis() - updatesCheckTime < 80) {
            Log.d("AppUpdates", "Abort updates check within 80ms.")
            return
        }
        updatesCheckTime = SystemClock.uptimeMillis()

        if (mutexUpdatesCheck.isLocked) return
        viewModelScope.launch(Dispatchers.IO) {
            if (mutexUpdatesCheck.isLocked) return@launch
            mutexUpdatesCheck.withLock check@{
                // update loading state
                mutUiState.update { it.copy(isLoading = true) }
                if (updatesOptions == null) {
                    updatesOptions = optionsOwner.getOptions(context)
                }
                // check all packages query permission first,
                // but continue querying anyway to retrieve partial result
                val perm = AppListPermission.queryAllPackagesOrNull(context)
                mutUiState.update { curr ->
                    curr.copy(perm = curr.perm.copy(
                        canQueryAllPkgs = perm == null,
                        queryPermission = perm,
                        canReqRuntimePerm = perm == AppListPermission.GetInstalledApps,
                    ))
                }

                val updatesChecker = updatesChecker ?: kotlin.run {
                    val pkgProvider = PlatformAppProvider(context)
                    val appRepo = appRepo ?: AppRepo.impl(context, pkgProvider).also { appRepo = it }
                    val updRepo = UpdateRepo.impl(context, appRepo, pkgProvider)
                    AppUpdatesChecker(updRepo).also { updatesChecker = it }
                }

                val sections = updatesChecker.checkNewUpdate(
                    changedLimit = 15 * columnCount,
                    usedLimit = if (columnCount <= 1) 5 else 6,
                    context = context
                )
                val updSections = mergeSections(uiState.value.sections, sections)

                sectionsAppList = updSections.flatMap { (_, list) ->
                    list.map(UpdatedApp::app)
                }
                mutUiState.update { it.copy(sections = updSections) }
                delay(100)  // delay finish loading state
                mutUiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun mergeSections(
        preSections: AppUpdatesSections, newSections: AppUpdatesSections): AppUpdatesSections {
        if (preSections.isEmpty()) return newSections
        if (newSections.isEmpty()) return preSections
        // merge all sections except USE from pre and new sections
        val mergedUpdates = EnumMap<AppUpdatesIndex, List<UpdatedApp>>(AppUpdatesIndex::class.java)
        AppUpdatesIndex.entries
            .filter { it != AppUpdatesIndex.USE }
            .forEach { i ->
                val (preList, newList) = preSections[i].orEmpty() to newSections[i].orEmpty()
                val newPkgs = newList.mapTo(HashSet(newList.size)) { it.app.packageName }
                val merged = newList + preList.filter { it.app.packageName !in newPkgs }
                if (merged.isNotEmpty()) mergedUpdates[i] = merged
            }
        // remove items from REC that are in other sections
        mergedUpdates[AppUpdatesIndex.REC]?.let { recList ->
            val updPkgs = buildSet {
                for ((i, list) in mergedUpdates) {
                    if (i == AppUpdatesIndex.REC) continue
                    for (upd in list) add(upd.app.packageName)
                }
            }
            if (updPkgs.isNotEmpty()) {
                val filteredRec = recList.filter { it.app.packageName !in updPkgs }
                if (filteredRec.isNotEmpty()) mergedUpdates[AppUpdatesIndex.REC] = filteredRec
                if (filteredRec.isEmpty()) mergedUpdates.remove(AppUpdatesIndex.REC)
            }
        }
        // add USE section from new sections
        val usedList = newSections[AppUpdatesIndex.USE]
        if (!usedList.isNullOrEmpty()) {
            // reuse existing records to avoid non-persistent tags' re-computations
            val preSet = preSections[AppUpdatesIndex.USE]?.associateBy { it.app.packageName }
            val reuser = { upd: UpdatedApp -> preSet!![upd.app.packageName] ?: upd }
            mergedUpdates[AppUpdatesIndex.USE] = preSet?.let { usedList.map(reuser) } ?: usedList
        }
        return mergedUpdates
    }

    fun getApp(context: Context, pkgName: String): ApiViewingApp? {
        return appRepo?.getApp(pkgName)
    }

    fun checkListPrefs(context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            if (optionsOwner.checkPrefsChanged(context)) {
                // trigger list prefs update
                mutAppListId.update { it + 1 }
            }
        }
    }
}
