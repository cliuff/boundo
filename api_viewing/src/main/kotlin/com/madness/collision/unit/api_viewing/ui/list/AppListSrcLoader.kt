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
import android.net.Uri
import com.madness.collision.chief.chiefContext
import com.madness.collision.unit.api_viewing.AppTag
import com.madness.collision.unit.api_viewing.apps.AppRepository
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.util.ApkRetriever
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface AppListLoader {
    val appRepo: AppRepository
    val apkRetriever: ApkRetriever
    fun getAppList(cat: ListSrcCat): OrderedAppList
}

class AppListSrcLoader(
    loader: AppListLoader,
    private val optionsOwner: AppListOptionsOwner,
) : AppListLoader by loader {

    private val ongoingSrc: MutableMap<AppListSrc, Job?> = hashMapOf()
    private val srcMutexMap: Map<ListSrcKey<*>, Mutex> = ListSrcKeys.associateWith { Mutex() }
    private val mutLoadingSrcFlow: MutableStateFlow<Set<AppListSrc>> = MutableStateFlow(emptySet())
    val loadingSrcFlow: StateFlow<Set<AppListSrc>> by ::mutLoadingSrcFlow

    private suspend fun load(src: AppListSrc, block: suspend () -> Unit): Job {
        val oldJob = ongoingSrc[src]
        if (oldJob?.isActive == true) return oldJob
        return srcMutexMap.getValue(src.key).withLock lock@{
            ongoingSrc[src]?.let { j -> if (j.isActive) return@lock j }
            coroutineScope {
                val job = launch { block() }
                ongoingSrc.put(src, job)?.cancel()
                job.invokeOnCompletion {
                    ongoingSrc.remove(src)
                    mutLoadingSrcFlow.tryEmit(ongoingSrc.filter { it.value?.isActive == true }.keys)
                }
                mutLoadingSrcFlow.emit(ongoingSrc.filter { it.value?.isActive == true }.keys)
                job
            }
        }
    }

    suspend fun addListSrc(src: AppListSrc) = channelFlow {
        load(src) {
            when (src) {
                AppListSrc.SystemApps -> addApps(src, ApiUnit.SYS)?.let { send(it) }
                AppListSrc.UserApps -> addApps(src, ApiUnit.USER)?.let { send(it) }
                AppListSrc.DeviceApks -> TODO()
                is AppListSrc.SelectApks -> // todo empty case
                    for (uri in src.uriList) addApk(src, uri)
                is AppListSrc.SelectVolume -> {
                    if (src.uri != null) {
                        apkRetriever.fromUri(src.uri) { addApk(src, it) }
                    }
                }
                AppListSrc.DragAndDrop -> TODO()
                is AppListSrc.TagFilter -> send(src.apply(chiefContext))
                is AppListSrc.DataSourceQuery -> {
                    getAppList(src.cat).addAllItems(src, appRepo.queryApps(src.value))
                    send(getAppList(src.cat).getList())
                }
            }
        }
    }

    /** Add both [AppListSrc.SystemApps] and [AppListSrc.UserApps] at once */
    suspend fun addPlatformSrc() = channelFlow {
        load(AppListSrc.SystemApps) {
            load(AppListSrc.UserApps) user@{
                val appList = getAppList(ListSrcCat.Platform)
                if (appList.containsSrc(AppListSrc.SystemApps)) return@user
                if (appList.containsSrc(AppListSrc.UserApps)) return@user
                val (list1, list2) = appRepo.getApps(ApiUnit.ALL_APPS)
                    .partition { it.apiUnit == ApiUnit.SYS }
                appList.addAllItems(AppListSrc.SystemApps, list1)
                appList.addAllItems(AppListSrc.UserApps, list2)
                send(appList.getList())
            }
        }
    }

    private fun addApps(src: AppListSrc, apiUnit: Int): List<ApiViewingApp>? {
        val appList = getAppList(src.cat)
        if (appList.containsSrc(src.key)) return null
        appList.addAllItems(src, appRepo.getApps(apiUnit))
        return appList.getList()
    }

    private fun SendChannel<List<ApiViewingApp>>.addApk(src: AppListSrc, apkUri: Uri) {
        apkRetriever.resolveUri(apkUri) { app ->
            getAppList(src.cat).addItem(src, app)
            trySend(getAppList(src.cat).getList())
        }
    }

    private suspend fun AppListSrc.TagFilter.apply(context: Context) = coroutineScope {
        val src = this@apply
        // cannot detect whether changed, previous state cannot be determined
        // because filter state share data with normal state
        optionsOwner.reloadTagSettings(src.checkedTags)
        if (src.checkedTags.isNotEmpty()) {
            // filter app list in parallel
            val appList = getAppList(src.targetCat).getList()
            val filterList = appList.map { async { AppTag.filterTags(context, it) } }.awaitAll()
            val filterResult = appList.filterIndexed { i, _ -> filterList[i] }
            getAppList(src.cat).addAllItems(src, filterResult)
            filterResult
        } else {
            getAppList(src.targetCat).getList()
        }
    }
}
