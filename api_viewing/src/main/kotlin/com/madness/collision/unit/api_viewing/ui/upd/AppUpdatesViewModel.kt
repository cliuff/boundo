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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.chief.auth.PermissionState
import com.madness.collision.unit.api_viewing.apps.AppListPermission
import com.madness.collision.unit.api_viewing.apps.AppRepo
import com.madness.collision.unit.api_viewing.apps.AppRepository
import com.madness.collision.unit.api_viewing.apps.PlatformAppProvider
import com.madness.collision.unit.api_viewing.apps.UpdateRepo
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.upgrade.Upgrade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

typealias AppUpdatesSections = Map<AppUpdatesIndex, List<*>>

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
        MutableStateFlow(AppUpdatesUiState(isLoading = false, sections = emptyMap(), perm = perm))
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
        private set
    var sectionsAppList: List<ApiViewingApp> = emptyList()
        private set

    fun setUpdatesColumnCount(columnCount: Int) {
        this.columnCount = columnCount.coerceAtLeast(1)
    }

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

    fun checkUpdates(timestamp: Long, context: Context, lifecycleOwner: LifecycleOwner) {
        if (uiState.value.isLoading) return
        viewModelScope.launch(Dispatchers.IO) {
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

                val appRepo = appRepo ?: AppRepo.impl(context, lifecycleOwner).also { appRepo = it }
                val updatesChecker = updatesChecker ?: kotlin.run {
                    val pkgProvider = PlatformAppProvider(context)
                    val updRepo = UpdateRepo.impl(context, appRepo, pkgProvider)
                    AppUpdatesChecker(updRepo).also { updatesChecker = it }
                }

                val sections = updatesChecker.checkNewUpdate(
                    changedLimit = 15 * columnCount,
                    usedLimit = if (columnCount <= 1) 5 else 6,
                    context = context
                )
                // todo preserve sections but exclude conflicts
                val updSections = uiState.value.sections + sections
                sectionsAppList = updSections.flatMap { (_, list) ->
                    list.mapNotNull { item ->
                        when (item) {
                            is ApiViewingApp -> item
                            is Upgrade -> item.new
                            else -> null
                        }
                    }
                }
                mutUiState.update { it.copy(isLoading = false, sections = updSections) }
            }
        }
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
