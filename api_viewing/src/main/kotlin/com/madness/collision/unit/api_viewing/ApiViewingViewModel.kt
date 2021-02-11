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
import android.net.Uri
import android.util.SparseIntArray
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.madness.collision.misc.MiscApp
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.database.DataMaintainer
import com.madness.collision.unit.api_viewing.util.ApkRetriever
import com.madness.collision.util.F
import com.madness.collision.util.StringUtils
import com.madness.collision.util.X
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.stream.Collectors
import kotlin.Comparator

/**
 * Api Viewing View Model
 */
internal class ApiViewingViewModel(application: Application): AndroidViewModel(application) {

    private val repository: AppRepository
//    var scrollPosition: Int = 0
    var loadedItems: ApiUnit = ApiUnit()
    /**
     * Timestamp for @see [ApiViewingViewModel.apps4Cache]
     */
    var timestampCache = 0L
    private set
    // for both internal and external access
    val apps4Cache: MutableList<ApiViewingApp> = mutableListOf()
    var appsCountUser: Int = 0
        private set
    var appsCountSystem: Int = 0
        private set
    val appsCountAll: Int
        get() = appsCountUser + appsCountSystem
    var apiCountUser: SparseIntArray = SparseIntArray(0)
        private set
    var apiCountSystem: SparseIntArray = SparseIntArray(0)
        private set
    var apiCountAll: SparseIntArray = SparseIntArray(0)
        private set
    var minApiCountUser: SparseIntArray = SparseIntArray(0)
        private set
    var minApiCountSystem: SparseIntArray = SparseIntArray(0)
        private set
    var minApiCountAll: SparseIntArray = SparseIntArray(0)
        private set
    val aiCount: Pair<Int, Int>
        get() {
            var countUser = 0
            var countSys = 0
            apps4Cache.forEach {
                if (it.adaptiveIcon) {
                    when (it.apiUnit) {
                        ApiUnit.USER -> countUser ++
                        ApiUnit.SYS -> countSys ++
                    }
                }
            }
            return countUser to countSys
        }

    init {
        val dao = DataMaintainer.get(application, viewModelScope)
        repository = AppRepository(dao)
    }

    private fun updateDeviceAppsCount(){
        var countAppsSystem = 0
        var countAppsUser = 0
        val apiCountMapUser: MutableMap<Int, Int> = mutableMapOf()
        val apiCountMapSystem: MutableMap<Int, Int> = mutableMapOf()
        val minApiCountMapUser: MutableMap<Int, Int> = mutableMapOf()
        val minApiCountMapSystem: MutableMap<Int, Int> = mutableMapOf()
        apps4Cache.forEach {
            when(it.apiUnit){
                ApiUnit.USER -> {
                    countAppsUser ++
                    val c = if (apiCountMapUser.containsKey(it.targetAPI)) apiCountMapUser.getValue(it.targetAPI) else 0
                    apiCountMapUser[it.targetAPI] = c + 1
                    val cm = if (minApiCountMapUser.containsKey(it.minAPI)) minApiCountMapUser.getValue(it.minAPI) else 0
                    minApiCountMapUser[it.minAPI] = cm + 1
                }
                ApiUnit.SYS -> {
                    countAppsSystem ++
                    val c = if (apiCountMapSystem.containsKey(it.targetAPI)) apiCountMapSystem.getValue(it.targetAPI) else 0
                    apiCountMapSystem[it.targetAPI] = c + 1
                    val cm = if (minApiCountMapSystem.containsKey(it.minAPI)) minApiCountMapSystem.getValue(it.minAPI) else 0
                    minApiCountMapSystem[it.minAPI] = cm + 1
                }
            }
        }
        appsCountUser = countAppsUser
        appsCountSystem = countAppsSystem
        updateDeviceAppsCountApi(true, apiCountMapUser, apiCountMapSystem)
        updateDeviceAppsCountApi(false, minApiCountMapUser, minApiCountMapSystem)
    }

    private fun updateDeviceAppsCountApi(isTarget: Boolean, apiCountMapUser: Map<Int, Int>, apiCountMapSystem: Map<Int, Int>){
        val appApiAll = SparseIntArray(apiCountMapUser.size + apiCountMapSystem.size)
        var appApi = SparseIntArray(apiCountMapUser.size)
        apiCountMapUser.forEach {
            appApi.put(it.key, it.value)
            val c = if (appApiAll.indexOfKey(it.key) != -1) appApiAll.get(it.key) else 0
            appApiAll.put(it.key, c + it.value)
        }
        if (isTarget) apiCountUser = appApi else minApiCountUser = appApi
        appApi = SparseIntArray(apiCountMapSystem.size)
        apiCountMapSystem.forEach {
            appApi.put(it.key, it.value)
            val c = if (appApiAll.indexOfKey(it.key) != -1) appApiAll.get(it.key) else 0
            appApiAll.put(it.key, c + it.value)
        }
        if (isTarget) {
            apiCountSystem = appApi
            apiCountAll = appApiAll
        } else {
            minApiCountSystem = appApi
            minApiCountAll = appApiAll
        }
    }

    fun updateApps4Cache(list: List<ApiViewingApp>, shouldUpdateCount: Boolean = true){
        apps4Cache.clear()
        apps4Cache.addAll(list)
        timestampCache = System.currentTimeMillis()
        if (shouldUpdateCount) updateDeviceAppsCount()
    }

    private fun doWork(block: suspend CoroutineScope.() -> Unit){
        viewModelScope.launch(Dispatchers.IO, block = block)
    }

    fun insert(app: ApiViewingApp){
        doWork { repository.insert(app) }
    }

    fun clearCache() {
        apps4Cache.clear()
        loadedItems.clear()
        timestampCache = System.currentTimeMillis()
        updateDeviceAppsCount()
    }

    fun addApps(vararg apps: ApiViewingApp) {
        apps4Cache.addAll(apps)
        timestampCache = System.currentTimeMillis()
        updateDeviceAppsCount()
    }

    fun addApps(list: List<ApiViewingApp>) {
        apps4Cache.addAll(list)
        timestampCache = System.currentTimeMillis()
        updateDeviceAppsCount()
    }

    fun addUserApps(context: Context) {
        apps4Cache.addAll(repository.getUserApps(context))
        timestampCache = System.currentTimeMillis()
        updateDeviceAppsCount()
    }

    fun addSystemApps(context: Context) {
        apps4Cache.addAll(repository.getSystemApps(context))
        timestampCache = System.currentTimeMillis()
        updateDeviceAppsCount()
    }

    fun addAllApps(context: Context) {
        apps4Cache.addAll(repository.getAllApps(context))
        timestampCache = System.currentTimeMillis()
        updateDeviceAppsCount()
    }

    fun get(packageName: String): ApiViewingApp?{
        return apps4Cache.find { it.packageName == packageName }
    }

    /**
     * Parse from file
     * @return The loaded APIApp object.
     * @param filePath The path to the file.
     */
    fun addFile(context: Context, filePath: String ) {
        val info = MiscApp.getPackageInfo(context, apkPath = filePath) ?: return
        ApiViewingApp(context, info, preloadProcess = true, archive = true)
                .initArchive(context, info.applicationInfo)
                .load(context, info.applicationInfo)
                .let { apps4Cache.add(it) }
        timestampCache = System.currentTimeMillis()
    }

    fun addFile(context: Context, fileUri: Uri) {
        val file = getFileFromUri(context, fileUri) ?: return
        if (file.isDirectory){
            val paths: MutableList<String> = ArrayList()
            X.listFiles(file, ".apk", paths)
            paths.forEach { addFile(context, it) }
        } else {
            addFile(context, file.path)
        }
    }

    private fun getFileFromUri(context: Context, uri: Uri): File?
    {
        uri.path.let { File(it ?: "").run { if (exists()) return this } }
        val fileName = "${ApkRetriever.APP_CACHE_PREFIX}${System.currentTimeMillis()}.apk"
        val file = F.createFile(F.cachePublicPath(context), "App", "Apk", fileName)
        if (!F.prepare4(file)) return null
        try {
            val inStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
            inStream.use { FileOutputStream(file).use { outStream -> it.copyTo(outStream) } }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
        return file
    }

    fun screen4Display(loadItem: Int): List<ApiViewingApp> {
        synchronized(apps4Cache){
            return screen2DisplayPrivate(loadItem, apps4Cache)
        }
    }

    private fun screen2DisplayPrivate(loadItem: Int, info: List<ApiViewingApp> ): List<ApiViewingApp> {
        if (ApiUnit.ineffective(loadItem)) return info
        val unit = if (loadItem == ApiUnit.SELECTED || loadItem == ApiUnit.VOLUME || loadItem == ApiUnit.DISPLAY) ApiUnit.APK else loadItem
        if (X.aboveOn(X.N)) {
            return if (loadItem == ApiUnit.ALL_APPS){
                info.parallelStream().filter{ item -> item.apiUnit != ApiUnit.APK }.collect(Collectors.toList())
            }else {
                info.parallelStream().filter{ item -> item.apiUnit == unit }.collect(Collectors.toList())
            }
        }else {
            val result: MutableList<ApiViewingApp> = mutableListOf()
            val iterator: Iterator<ApiViewingApp> = info.iterator()
            if (loadItem == ApiUnit.ALL_APPS){
                while (iterator.hasNext()){
                    val item = iterator.next()
                    if (item.apiUnit != ApiUnit.APK)
                        result.add(item)
                }
            }else {
                while (iterator.hasNext()){
                    val item = iterator.next()
                    if (item.apiUnit == unit)
                        result.add(item)
                }
            }
            return result
        }
    }

    fun screenOut(unit: Int) {
        updateApps4Cache(if (unit == ApiUnit.ALL_APPS){
            apps4Cache.filter { it.apiUnit == ApiUnit.APK }
        }else {
            apps4Cache.filter { it.apiUnit != unit }
        })
    }

    fun sortApps(sortItem: Int) {
        updateApps4Cache(sortList(apps4Cache, sortItem), false)
    }

    fun findApps(searchSize: Int, predicate: (app: ApiViewingApp) -> Boolean): MutableList<ApiViewingApp> {
        return if (X.aboveOn(X.N)) {
            apps4Cache.parallelStream().filter(predicate).collect(Collectors.toList())
        } else {
            val list = ArrayList<ApiViewingApp>(searchSize)
            for (listApp in apps4Cache) {
                if (predicate.invoke(listApp)) {
                    list.add(listApp)
                }
                if (list.size == searchSize) break
            }
            list
        }
    }

    companion object {

        private fun compareName(o1: ApiViewingApp, o2: ApiViewingApp): Int {
            return StringUtils.compareName(o1.name, o2.name)
        }

        private fun compareApi(list: List<ApiViewingApp>, isTarget: Boolean, isAsc: Boolean): List<ApiViewingApp> {
            val compareInt = if (isTarget) ApiViewingApp::targetAPI else ApiViewingApp::minAPI
            val compareDouble = if (isTarget) ApiViewingApp::targetSDKDouble else ApiViewingApp::minSDKDouble
            return if (X.aboveOn(X.N)) {
                val comparator = Comparator.comparingInt(compareInt)
                        .thenComparingDouble(compareDouble).run {
                            if (isAsc) this else reversed()
                        }.thenComparing(this::compareName)
                list.parallelStream().sorted(comparator).collect(Collectors.toList())
            } else {
                list.toMutableList().apply {
                    sortWith(Comparator { o1, o2 ->
                        val obj1 = if (isAsc) o1 else o2
                        val obj2 = if (isAsc) o2 else o1
                        val apiCompare = compareInt.invoke(obj1).compareTo(compareInt.invoke(obj2))
                        if (apiCompare != 0) return@Comparator apiCompare
                        val apiLevelCompare = compareDouble.invoke(obj1).compareTo(compareDouble.invoke(obj2))
                        if (apiLevelCompare != 0) return@Comparator apiLevelCompare
                        // name comparing is not reversed
                        compareName(o1, o2)
                    })
                }
            }
        }

        private fun compareName(list: List<ApiViewingApp>): List<ApiViewingApp> {
            return list.toMutableList().apply {
                sortWith(Comparator(this@Companion::compareName))
            }
        }

        private fun compareTime(list: List<ApiViewingApp>): List<ApiViewingApp> {
            return if (X.aboveOn(X.N)) {
                val comparator: Comparator<ApiViewingApp> = Comparator.comparingLong(ApiViewingApp::updateTime)/*
                        .thenComparingInt(APIApp::targetAPI)
                        .thenComparingDouble(APIApp::targetSDK)*/
                        .reversed()
                        .thenComparing(this::compareName)
                list.parallelStream().sorted(comparator).collect(Collectors.toList())
            } else {
                list.toMutableList().apply {
                    sortWith(Comparator { o1, o2 ->
                        val compareTime = o2.updateTime.compareTo(o1.updateTime)
                        if (compareTime != 0) return@Comparator compareTime
                        return@Comparator compareName(o1, o2)
                    })
                }
            }
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

}
