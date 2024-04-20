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

package com.madness.collision.unit.api_viewing.ui.list

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.unit.api_viewing.AppListStats
import com.madness.collision.unit.api_viewing.AppStatsTracker
import com.madness.collision.unit.api_viewing.MyUpdatesFragment
import com.madness.collision.unit.api_viewing.apps.AppListRepoImpl
import com.madness.collision.unit.api_viewing.apps.AppListRepository
import com.madness.collision.unit.api_viewing.apps.AppRepoImpl
import com.madness.collision.unit.api_viewing.apps.AppRepository
import com.madness.collision.unit.api_viewing.apps.CodingArtifact
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.DataMaintainer
import com.madness.collision.unit.api_viewing.util.ApkRetriever
import com.madness.collision.util.ui.PackageInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Graphical UI Artifact */
data class GuiArt(val packageName: String, val label: String, val iconPkgInfo: PackageInfo?)

private fun List<CodingArtifact>.toGui(): List<GuiArt> {
    return map { a ->
        //val iconInfo = AppIconPackageInfo(TODO())
        GuiArt(
            packageName = a.packageName,
            label = a.labelOrNull ?: a.packageName,
            iconPkgInfo = null,
        )
    }
}

data class AppListOptions(
    val srcSet: List<AppListSrc>,
    val listOrder: AppListOrder,
    val apiMode: AppApiMode,
)

typealias AppListUiState = List<GuiArt>
data class AppListOpUiState(val options: AppListOptions)

class AppListViewModel : ViewModel() {
    companion object {
        private var mutAppListStats: AppListStats? = null
        val appListStats: AppListStats? by ::mutAppListStats
    }

    private val appListRepo: AppListRepository = AppListRepoImpl()
    private val mutUiState = MutableStateFlow<AppListUiState>(emptyList())
    val uiState: StateFlow<AppListUiState> by ::mutUiState

    @Deprecated(message = "")
    private val mutAppList = MutableStateFlow<List<ApiViewingApp>>(emptyList())
    @Deprecated(message = "")
    val appList: StateFlow<List<ApiViewingApp>> by ::mutAppList
    private val multiSrcApps: MultiSrcApps
    /** the category that is eventually displayed */
    private var terminalSrcCat: ListSrcCat = ListSrcCat.Platform

    private val mutOpUiState: MutableStateFlow<AppListOpUiState>
    val opUiState: StateFlow<AppListOpUiState> by ::mutOpUiState
    private val optionsOwner = AppListOptionsOwner()
    private lateinit var srcLoader: AppListSrcLoader
    private val appStatsTracker = AppStatsTracker()

    val isLoadingSrc: Flow<Boolean>
        get() = srcLoader.loadingSrcFlow.map { it.isNotEmpty() }

    init {
        val options = AppListOptions(emptyList(), AppListOrder.UpdateTime, AppApiMode.Target)
        multiSrcApps = MultiSrcApps(options.listOrder, options.apiMode)
        mutOpUiState = MutableStateFlow(AppListOpUiState(options))
        appListRepo.apps.onEach { r -> mutUiState.update { r.toGui() } }.launchIn(viewModelScope)
//        viewModelScope.launch(Dispatchers.Default) { appListRepo.fetchNewData(chiefPkgMan) }
    }

    override fun onCleared() {
        clearCache()
        super.onCleared()
    }

    fun clearCache() {
        multiSrcApps.clearAll()
        mutAppListStats = AppListStats()
    }

    private fun updateSrcApps() {
    }

    fun init(context: Context, lifecycleOwner: LifecycleOwner, sessionTimestamp: Long) {
        val dao = DataMaintainer.get(context, lifecycleOwner)
        val appRepo = AppRepoImpl(dao, lifecycleOwner)
        val loader = object : AppListLoader {
            override val appRepo: AppRepository = appRepo
            override val apkRetriever: ApkRetriever = ApkRetriever(context)
            override fun getAppList(cat: ListSrcCat): OrderedAppList = multiSrcApps[cat]
        }
        srcLoader = AppListSrcLoader(loader, optionsOwner)

        viewModelScope.launch(Dispatchers.Default) {
            if (MyUpdatesFragment.isNewSession(sessionTimestamp)) {
                appRepo.maintainRecords(context)
            }
            val options = optionsOwner.getOptions(context)
            mutOpUiState.update { AppListOpUiState(options) }

            multiSrcApps[ListSrcCat.Platform].setOptions(options.listOrder, options.apiMode)
            options.srcSet.forEach { src ->
                srcLoader.addListSrc(src)
                    .onEach {
                        if (src.cat == ListSrcCat.Platform) {
                            val platformList = multiSrcApps[ListSrcCat.Platform].getList()
                            launch { mutAppListStats = appStatsTracker.updateDeviceAppsCount(platformList) }
                        }
                    }
                    .filter { terminalSrcCat == src.cat }
                    .onEach { updateList -> mutAppList.update { updateList } }
                    .catch { it.printStackTrace() }
                    .launchIn(this)
            }
        }
    }

    fun containsListSrc(srcKey: ListSrcKey<*>): Boolean {
        return multiSrcApps[srcKey.cat].containsSrc(srcKey)
    }

    fun removeListSrc(srcKey: ListSrcKey<*>) {
        opUiState.value.options.srcSet
            .filter { it.key == srcKey }
            .forEach(::toggleListSrc)
    }

    fun toggleListSrc(src: AppListSrc) {
        val srcSet = opUiState.value.options.srcSet.toHashSet()
        val newSrcSet = if (src in srcSet) srcSet - src else srcSet + src
        mutOpUiState.update {
            opUiState.value.run { copy(options = options.copy(srcSet = newSrcSet.toList())) }
        }
        viewModelScope.launch(Dispatchers.Default) {
            optionsOwner.setListSrc(src, newSrcSet)
            if (src in srcSet) {
                multiSrcApps[src.cat].removeAppSrc(src)
                if (src.cat == ListSrcCat.Platform) {
                    val platformList = multiSrcApps[ListSrcCat.Platform].getList()
                    launch { mutAppListStats = appStatsTracker.updateDeviceAppsCount(platformList) }
                }
                val srcCat = src.cat.takeIf { multiSrcApps[it].isNotEmpty() }
                    ?: ListSrcCat.entries.find { multiSrcApps[it].isNotEmpty() }
                    ?: ListSrcCat.Platform
                terminalSrcCat = srcCat
                mutAppList.update { multiSrcApps[srcCat].getList() }
            } else {
                srcLoader.addListSrc(src)
                    .onEach {
                        if (src.cat == ListSrcCat.Platform) {
                            val platformList = multiSrcApps[ListSrcCat.Platform].getList()
                            launch { mutAppListStats = appStatsTracker.updateDeviceAppsCount(platformList) }
                        }
                    }
                    .filter { terminalSrcCat == src.cat }
                    .onEach { updateList -> mutAppList.update { updateList } }
                    .catch { it.printStackTrace() }
                    .launchIn(this)
                terminalSrcCat = src.cat
                mutAppList.update { multiSrcApps[src.cat].getList() }
            }
        }
    }

    fun setListOrder(order: AppListOrder) {
        if (order == opUiState.value.options.listOrder) return
        mutOpUiState.update {
            opUiState.value.run { copy(options = options.copy(listOrder = order)) }
        }
        viewModelScope.launch(Dispatchers.Default) {
            optionsOwner.setListOrder(order)
            val sortedList = multiSrcApps[terminalSrcCat].setOptions(order = order).getList()
            mutAppList.update { sortedList }
        }
    }

    fun setApiMode(apiMode: AppApiMode) {
        if (apiMode == opUiState.value.options.apiMode) return
        mutOpUiState.update {
            opUiState.value.run { copy(options = options.copy(apiMode = apiMode)) }
        }
        viewModelScope.launch(Dispatchers.Default) {
            optionsOwner.setApiMode(apiMode)
            val sortedList = multiSrcApps[terminalSrcCat].setOptions(apiMode = apiMode).getList()
            mutAppList.update { sortedList }
        }
    }
}
