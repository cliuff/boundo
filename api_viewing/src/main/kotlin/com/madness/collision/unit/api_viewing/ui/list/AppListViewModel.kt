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
import com.madness.collision.unit.api_viewing.apps.AppListRepoImpl
import com.madness.collision.unit.api_viewing.apps.AppListRepository
import com.madness.collision.unit.api_viewing.apps.AppRepoImpl
import com.madness.collision.unit.api_viewing.apps.AppRepository
import com.madness.collision.unit.api_viewing.apps.AppUpdatesLists
import com.madness.collision.unit.api_viewing.apps.CodingArtifact
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.DataMaintainer
import com.madness.collision.unit.api_viewing.util.ApkRetriever
import com.madness.collision.util.F
import com.madness.collision.util.ui.PackageInfo
import com.opencsv.CSVWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.FileWriter

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

data class AppSrcState(
    val terminalCat: ListSrcCat,
    val isLoadingSrc: Boolean,
    val loadedCats: Set<ListSrcCat>,
)

sealed interface AppListEvent {
    class ShareAppList(val file: File) : AppListEvent
}

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
    private val mutAppListId = MutableStateFlow(0)
    val appListId: StateFlow<Int> by ::mutAppListId
    private val mutAppsModNotifier = MutableStateFlow(0)
    private val mutSrcState: MutableStateFlow<AppSrcState>
    val appSrcState: StateFlow<AppSrcState> by ::mutSrcState
    private val multiSrcApps: MultiSrcApps
    /** the category that is eventually displayed */
    private val terminalSrcCat: ListSrcCat
        get() = appSrcState.value.terminalCat

    private val mutOpUiState: MutableStateFlow<AppListOpUiState>
    val opUiState: StateFlow<AppListOpUiState> by ::mutOpUiState
    private val optionsOwner = AppListOptionsOwner()
    private lateinit var srcLoader: AppListSrcLoader
    private val appStatsTracker = AppStatsTracker()
    private val mutEvents = MutableSharedFlow<AppListEvent>()
    val events: SharedFlow<AppListEvent> = mutEvents.asSharedFlow()

    init {
        val options = AppListOptions(emptyList(), AppListOrder.UpdateTime, AppApiMode.Target)
        multiSrcApps = MultiSrcApps(options.listOrder, options.apiMode)
        mutOpUiState = MutableStateFlow(AppListOpUiState(options))
        val appSrcState = AppSrcState(ListSrcCat.Platform, false, emptySet())
        mutSrcState = MutableStateFlow(appSrcState)
        appListRepo.apps.onEach { r -> mutUiState.update { r.toGui() } }.launchIn(viewModelScope)
//        viewModelScope.launch(Dispatchers.Default) { appListRepo.fetchNewData(chiefPkgMan) }
    }

    override fun onCleared() {
        multiSrcApps.clearAll()
        mutAppListStats = AppListStats()
        super.onCleared()
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
            if (AppUpdatesLists.updatesSession.isNewSession(sessionTimestamp)) {
                appRepo.maintainRecords(context)
            }
            val options = optionsOwner.getOptions(context)
            mutOpUiState.update { AppListOpUiState(options) }

            val srcFlow = combine(srcLoader.loadingSrcFlow, mutAppsModNotifier) { loadingSrcSet, _ ->
                loadingSrcSet.isNotEmpty() to
                        ListSrcCat.entries.filter { c -> multiSrcApps[c].isNotEmpty() }.toSet()
            }
            srcFlow
                .onEach src@{ (isLoadingSrc, loadedCats) ->
                    mutSrcState.update { currValue ->
                        val e1 = isLoadingSrc == currValue.isLoadingSrc
                        val e2 = loadedCats == currValue.loadedCats
                        when {
                            e1 && e2 -> currValue
                            e2 -> currValue.copy(isLoadingSrc = isLoadingSrc)
                            else -> currValue.copy(isLoadingSrc = isLoadingSrc, loadedCats = loadedCats)
                        }
                    }
                }
                .launchIn(this)

            multiSrcApps[ListSrcCat.Platform].setOptions(options.listOrder, options.apiMode)
            val flows = buildList(options.srcSet.size) {
                val (pList, otherList) = options.srcSet.partition { it.cat == ListSrcCat.Platform }
                if (pList.size >= 2) {
                    add(ListSrcCat.Platform to srcLoader.addPlatformSrc())
                    otherList.forEach { src -> add(src.cat to srcLoader.addListSrc(src)) }
                } else {
                    options.srcSet.forEach { src -> add(src.cat to srcLoader.addListSrc(src)) }
                }
            }
            flows.forEach { (srcCat, flow) ->
                flow
                    .onEach {
                        if (srcCat == ListSrcCat.Platform) {
                            val platformList = multiSrcApps[ListSrcCat.Platform].getList()
                            launch { mutAppListStats = appStatsTracker.updateDeviceAppsCount(platformList) }
                        }
                    }
                    .filter { terminalSrcCat == srcCat }
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

    fun setListSrcCat(cat: ListSrcCat) {
        mutSrcState.update { it.copy(terminalCat = cat) }
        mutAppList.update { multiSrcApps[cat].getList() }
    }

    fun toggleListSrc(src: AppListSrc, onFinish: (() -> Unit)? = null) {
        val srcSet = opUiState.value.options.srcSet.toHashSet()
        val newSrcSet = when (src.cat == ListSrcCat.Filter) {
            // remove all other Filters so that only one filter can be active at a time
            true -> if (src in srcSet) srcSet - src else srcSet.apply { removeIf { it.cat == ListSrcCat.Filter } } + src
            else -> if (src in srcSet) srcSet - src else srcSet + src
        }
        mutOpUiState.update {
            opUiState.value.run { copy(options = options.copy(srcSet = newSrcSet.toList())) }
        }
        viewModelScope.launch(Dispatchers.Default) {
            // try to wait for init to complete (otherwise abort) when called from LaunchMethod
            if (!::srcLoader.isInitialized) withTimeout(200) {
                while (!::srcLoader.isInitialized) delay(5)
            }
            optionsOwner.setListSrc(src, newSrcSet)
            if (src in srcSet) {
                multiSrcApps[src.cat].removeAppSrc(src)
                mutAppsModNotifier.update { it + 1 }
                if (src.cat == ListSrcCat.Platform) {
                    val platformList = multiSrcApps[ListSrcCat.Platform].getList()
                    launch { mutAppListStats = appStatsTracker.updateDeviceAppsCount(platformList) }
                }
                val srcCat = src.cat.takeIf { multiSrcApps[it].isNotEmpty() }
                    ?: ListSrcCat.entries.find { multiSrcApps[it].isNotEmpty() }
                    ?: ListSrcCat.Platform
                mutSrcState.update { it.copy(terminalCat = srcCat) }
                mutAppList.update { multiSrcApps[srcCat].getList() }
            } else {
                // only one filter can be active at a time
                if (src.cat == ListSrcCat.Filter) {
                    multiSrcApps[ListSrcCat.Filter].clearAll()
                }
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
                    .invokeOnCompletion { onFinish?.invoke() }
                mutSrcState.update { it.copy(terminalCat = src.cat) }
                mutAppList.update { multiSrcApps[src.cat].getList() }
            }
        }
    }

    fun setQueryFilter(query: CharSequence) {
        val srcSet = opUiState.value.options.srcSet
        val filter = srcSet.filterIsInstance<AppListSrc.DataSourceQuery>().firstOrNull()
        if (query.isBlank()) {
            // remove existing src instance
            filter?.let(::toggleListSrc)
        } else if (terminalSrcCat == AppListSrc.DataSourceQuery.cat) {
            val cat = when (val src = srcSet.find { it.cat == ListSrcCat.Filter }) {
                is AppListSrc.DataSourceQuery -> src.targetCat
                is AppListSrc.TagFilter -> src.targetCat
                else -> error("ListSrcCat.Filter not found or matched")
            }
            toggleListSrc(AppListSrc.DataSourceQuery(cat, query.toString()))
        } else {
            toggleListSrc(AppListSrc.DataSourceQuery(terminalSrcCat, query.toString()))
        }
    }

    fun updateTagFilter(tagId: String, checkedState: Boolean?) {
        val srcSet = opUiState.value.options.srcSet
        val filter = srcSet.filterIsInstance<AppListSrc.TagFilter>().firstOrNull()
        val updatedTags = filter?.checkedTags.orEmpty()
            .run { if (checkedState == null) minus(tagId) else plus(tagId to checkedState) }
        if (updatedTags.isEmpty()) {
            optionsOwner.reloadTagSettings()
            // remove existing src instance
            filter?.let(::toggleListSrc)
        } else if (terminalSrcCat == AppListSrc.TagFilter.cat) {
            val cat = when (val src = srcSet.find { it.cat == ListSrcCat.Filter }) {
                is AppListSrc.DataSourceQuery -> src.targetCat ?: ListSrcCat.Platform
                is AppListSrc.TagFilter -> src.targetCat
                else -> error("ListSrcCat.Filter not found or matched")
            }
            toggleListSrc(AppListSrc.TagFilter(cat, updatedTags))
        } else {
            toggleListSrc(AppListSrc.TagFilter(terminalSrcCat, updatedTags))
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

    fun checkListPrefs(context: Context) {
        viewModelScope.launch(Dispatchers.Default) {
            if (optionsOwner.checkPrefsChanged(context)) {
                // trigger list prefs update
                mutAppListId.update { it + 1 }
            }
        }
    }

    fun exportAppList(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val apps = multiSrcApps[terminalSrcCat].getList()
            val name = "AppList"
            val path = F.createPath(F.cachePublicPath(context), "Temp", "AV", "$name.csv")
            val file = File(path)
            if (!F.prepare4(file)) return@launch
            CSVWriter(FileWriter(file)).use { csv ->
                apps.forEach { app ->
                    csv.writeNext(arrayOf(app.name))
                }
            }
            mutEvents.emit(AppListEvent.ShareAppList(file))
        }
    }
}
