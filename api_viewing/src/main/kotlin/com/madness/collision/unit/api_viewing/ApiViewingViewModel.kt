/*
 * Copyright 2021 Clifford Liu
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

package com.madness.collision.unit.api_viewing

import android.app.Application
import android.content.Context
import androidx.lifecycle.*
import com.madness.collision.unit.api_viewing.apps.AppRepoImpl
import com.madness.collision.unit.api_viewing.apps.AppRepository
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.database.DataMaintainer
import com.madness.collision.util.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.stream.Collectors

/**
 * Api Viewing View Model
 */
internal class ApiViewingViewModel(application: Application): AndroidViewModel(application) {

    companion object {
        private var mutAppListStats: AppListStats? = null
        val appListStats: AppListStats? by ::mutAppListStats

        fun sortList(list: List<ApiViewingApp>, sortItem: Int): List<ApiViewingApp> {
            return AppListComparator().sortList(list, sortItem)
        }
    }

    private val repository: AppRepository
    private val appListCache = AppListCache()
    private val appStatsTracker = AppStatsTracker()

    val loadedItems: ApiUnit = ApiUnit()
    val apps4Cache: List<ApiViewingApp> by ::appListCache

    private val lifecycleOwner: LifecycleOwner

    init {
        val lifecycleRegistry: LifecycleRegistry
        lifecycleOwner = object : LifecycleOwner {
            init { lifecycleRegistry = LifecycleRegistry(this) }
            override val lifecycle: Lifecycle = lifecycleRegistry
        }
        viewModelScope.launch(Dispatchers.Main) {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }
        val dao = DataMaintainer.get(application, lifecycleOwner)
        repository = AppRepoImpl(dao, lifecycleOwner)
    }

    override fun onCleared() {
        viewModelScope.launch(Dispatchers.Main) {
            val lifecycleRegistry = lifecycleOwner.lifecycle as LifecycleRegistry
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        val appCache = apps4Cache
        synchronized(appCache) {
            val iterator = appCache.iterator()
            while (iterator.hasNext()) {
                iterator.next().clearIcons()
            }
        }
        clearCache()
        super.onCleared()
    }

    fun clearCache() {
        appListCache.clearAll()
        loadedItems.clear()
        mutAppListStats = appStatsTracker.updateDeviceAppsCount(apps4Cache)
    }

    fun addApps(vararg apps: ApiViewingApp) {
        addApps(apps.asList())
    }

    fun addApps(list: List<ApiViewingApp>) {
        appListCache.add(list)
        mutAppListStats = appStatsTracker.updateDeviceAppsCount(apps4Cache)
    }

    fun addArchiveApp(app: ApiViewingApp) {
        appListCache.add(app)
    }

    private fun addApps(unit: Int) = addApps(repository.getApps(unit))
    fun addUserApps(context: Context) = addApps(ApiUnit.USER)
    fun addSystemApps(context: Context) = addApps(ApiUnit.SYS)
    fun addAllApps(context: Context) = addApps(ApiUnit.ALL_APPS)

    fun get(packageName: String): ApiViewingApp?{
        return apps4Cache.find { it.packageName == packageName }
    }

    fun maintainRecords(context: Context) {
        repository.maintainRecords(context)
    }


    //
    // App List Filtering
    //

    fun screen4Display(loadItem: Int): List<ApiViewingApp> {
        synchronized(apps4Cache){
            val info = apps4Cache
            if (ApiUnit.ineffective(loadItem)) return info
            val predicate = when (loadItem) {
                ApiUnit.SELECTED, ApiUnit.VOLUME, ApiUnit.DISPLAY -> { a -> a.apiUnit == ApiUnit.APK }
                ApiUnit.ALL_APPS -> { a -> a.apiUnit != ApiUnit.APK }
                else -> { a: ApiViewingApp -> a.apiUnit == loadItem }
            }
            return info.parallelStream().filter(predicate).collect(Collectors.toList())
        }
    }

    fun screenOut(unit: Int) {
        appListCache.replaceAs(when (unit) {
            ApiUnit.ALL_APPS -> apps4Cache.filter { it.apiUnit == ApiUnit.APK }
            else -> apps4Cache.filter { it.apiUnit != unit }
        })
        mutAppListStats = appStatsTracker.updateDeviceAppsCount(apps4Cache)
    }

    fun sortApps(sortItem: Int) {
        appListCache.replaceAs(sortList(apps4Cache, sortItem))
    }
}

class AppListComparator {
    private val nameComparator get() = compareBy(StringUtils.comparator, ApiViewingApp::name)

    private fun compareApi(list: List<ApiViewingApp>, isTarget: Boolean, isAsc: Boolean): List<ApiViewingApp> {
        val compareInt = if (isTarget) ApiViewingApp::targetAPI else ApiViewingApp::minAPI
        val comparator = compareBy(compareInt)
            .let { if (isAsc) it else it.reversed() }
            .then(nameComparator)
        return list.parallelStream().sorted(comparator).collect(Collectors.toList())
    }

    private fun compareName(list: List<ApiViewingApp>): List<ApiViewingApp> {
        return list.toMutableList().apply { sortWith(nameComparator) }
    }

    private fun compareTime(list: List<ApiViewingApp>): List<ApiViewingApp> {
        val comparator = compareBy(ApiViewingApp::updateTime).reversed().then(nameComparator)
        return list.parallelStream().sorted(comparator).collect(Collectors.toList())
    }

    fun sortList(list: List<ApiViewingApp>, sortItem: Int): List<ApiViewingApp> {
        if (list.isEmpty()) return emptyList()
        return when (sortItem) {
            MyUnit.SORT_POSITION_API_LOW -> compareApi(list, EasyAccess.isViewingTarget, true)
            MyUnit.SORT_POSITION_API_HIGH -> compareApi(list, EasyAccess.isViewingTarget, false)
            MyUnit.SORT_POSITION_API_NAME -> compareName(list)
            MyUnit.SORT_POSITION_API_TIME -> compareTime(list)
            else -> ArrayList(list)
        }
    }
}

class AppListCache(private val cacheList: MutableList<ApiViewingApp> = mutableListOf()) : List<ApiViewingApp> by cacheList {
    private var mutTimestamp = 0L
    val timestamp: Long by ::mutTimestamp

    fun add(app: ApiViewingApp) {
        cacheList.add(app)
        mutTimestamp = System.currentTimeMillis()
    }

    fun add(list: List<ApiViewingApp>) {
        cacheList.addAll(list)
        mutTimestamp = System.currentTimeMillis()
    }

    fun replaceAs(list: List<ApiViewingApp>) {
        cacheList.clear()
        cacheList.addAll(list)
        mutTimestamp = System.currentTimeMillis()
    }

    fun clearAll() {
        cacheList.clear()
        mutTimestamp = System.currentTimeMillis()
    }
}
