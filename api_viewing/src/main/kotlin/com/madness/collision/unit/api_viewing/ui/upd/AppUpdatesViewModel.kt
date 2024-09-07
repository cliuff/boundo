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
import com.madness.collision.unit.api_viewing.apps.AppRepo
import com.madness.collision.unit.api_viewing.apps.AppRepository
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
data class AppUpdatesUiState(val isLoading: Boolean, val sections: AppUpdatesSections)

class AppUpdatesViewModel : ViewModel() {
    private val mutUiState: MutableStateFlow<AppUpdatesUiState> =
        MutableStateFlow(AppUpdatesUiState(false, emptyMap()))
    val uiState: StateFlow<AppUpdatesUiState> = mutUiState.asStateFlow()
    private var appRepo: AppRepository? = null
    private val updatesChecker = AppUpdatesChecker()
    private val mutexUpdatesCheck = Mutex()
    var columnCount: Int = 1
        private set
    var sectionsAppList: List<ApiViewingApp> = emptyList()
        private set

    fun setUpdatesColumnCount(columnCount: Int) {
        this.columnCount = columnCount.coerceAtLeast(1)
    }

    fun checkUpdates(timestamp: Long, context: Context, lifecycleOwner: LifecycleOwner) {
        if (uiState.value.isLoading) return
        viewModelScope.launch(Dispatchers.IO) {
            mutexUpdatesCheck.withLock check@{
                mutUiState.update { it.copy(isLoading = true) }
                if (updatesChecker.isCheckNeeded()) {
                    updatesChecker.checkNewUpdate(timestamp, context, lifecycleOwner)
                }
                val sections = updatesChecker.getSections(
                    changedLimit = 15 * columnCount,
                    usedLimit = if (columnCount <= 1) 5 else 6,
                    context = context
                )
                sectionsAppList = sections.flatMap { (_, list) ->
                    list.mapNotNull { item ->
                        when (item) {
                            is ApiViewingApp -> item
                            is Upgrade -> item.new
                            else -> null
                        }
                    }
                }
                mutUiState.update { it.copy(isLoading = false, sections = sections) }
            }
        }
    }

    fun getApp(context: Context, pkgName: String): ApiViewingApp? {
        val repo = appRepo ?: AppRepo.dumb(context).also { appRepo = it }
        return repo.getApp(pkgName)
    }
}
