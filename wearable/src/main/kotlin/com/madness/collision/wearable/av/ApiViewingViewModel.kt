package com.madness.collision.wearable.av

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.SparseIntArray
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.madness.collision.wearable.av.data.ApiUnit
import com.madness.collision.wearable.av.data.ApiViewingApp
import com.madness.collision.wearable.util.X
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.text.Collator
import java.util.*
import java.util.stream.Collectors
import kotlin.Comparator
import kotlin.coroutines.CoroutineContext

/**
 * Api Viewing View Model
 */
internal class ApiViewingViewModel(application: Application): AndroidViewModel(application) {

    private val repository: AppRepository = AppRepository()
//    var scrollPosition: Int = 0
    var loadedItems: ApiUnit = ApiUnit()
    // for both internal and external access
    val apps4Cache: MutableList<ApiViewingApp> = mutableListOf()
    // for internal access
    private val apps4DisplayInternal: MutableLiveData<List<ApiViewingApp>> = MutableLiveData(emptyList())
    // for external access
    val apps4Display: LiveData<List<ApiViewingApp>>
        get() = apps4DisplayInternal
    // shortcut for external access
    val apps4DisplayValue: List<ApiViewingApp>
        get() = apps4Display.value ?: emptyList()
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

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Main
    private val scope = CoroutineScope(coroutineContext)

    override fun onCleared() {
        super.onCleared()
        parentJob.cancel()
    }

    fun updateApps4Display(list: List<ApiViewingApp> = apps4Cache){
        scope.launch { apps4DisplayInternal.value = list.toList() }
    }

    private fun updateDeviceAppsCount(){
        var countAppsSystem = 0
        var countAppsUser = 0
        val apiCountMapUser: MutableMap<Int, Int> = mutableMapOf()
        val apiCountMapSystem: MutableMap<Int, Int> = mutableMapOf()
        apps4Cache.forEach {
            when(it.apiUnit){
                ApiUnit.USER -> {
                    countAppsUser ++
                    val c = if (apiCountMapUser.containsKey(it.targetAPI)) apiCountMapUser.getValue(it.targetAPI) else 0
                    apiCountMapUser[it.targetAPI] = c + 1
                }
                ApiUnit.SYS -> {
                    countAppsSystem ++
                    val c = if (apiCountMapSystem.containsKey(it.targetAPI)) apiCountMapSystem.getValue(it.targetAPI) else 0
                    apiCountMapSystem[it.targetAPI] = c + 1
                }
            }
        }
        appsCountUser = countAppsUser
        appsCountSystem = countAppsSystem
        updateDeviceAppsCountApi(apiCountMapUser, apiCountMapSystem)
    }

    private fun updateDeviceAppsCountApi(apiCountMapUser: Map<Int, Int>, apiCountMapSystem: Map<Int, Int>){
        val appApiAll = SparseIntArray(apiCountMapUser.size + apiCountMapSystem.size)
        var appApi = SparseIntArray(apiCountMapUser.size)
        apiCountMapUser.forEach {
            appApi.put(it.key, it.value)
            val c = if (appApiAll.indexOfKey(it.key) != -1) appApiAll.get(it.key) else 0
            appApiAll.put(it.key, c + it.value)
        }
        apiCountUser = appApi
        appApi = SparseIntArray(apiCountMapSystem.size)
        apiCountMapSystem.forEach {
            appApi.put(it.key, it.value)
            val c = if (appApiAll.indexOfKey(it.key) != -1) appApiAll.get(it.key) else 0
            appApiAll.put(it.key, c + it.value)
        }
        apiCountSystem = appApi
        apiCountAll = appApiAll
    }

    fun updateApps4Cache(list: List<ApiViewingApp>, shouldUpdateCount: Boolean = true){
        apps4Cache.clear()
        apps4Cache.addAll(list)
        if (shouldUpdateCount) updateDeviceAppsCount()
    }

    private fun doWork(block: suspend CoroutineScope.() -> Unit){
        scope.launch(Dispatchers.IO, block = block)
    }

    fun addApps(vararg apps: ApiViewingApp) {
        apps4Cache.addAll(apps)
        updateDeviceAppsCount()
    }

    fun addApps(list: List<ApiViewingApp>) {
        apps4Cache.addAll(list)
        updateDeviceAppsCount()
    }

    fun addUserApps(context: Context) {
        apps4Cache.addAll(repository.getUserApps(context))
        updateDeviceAppsCount()
    }

    fun addSystemApps(context: Context) {
        apps4Cache.addAll(repository.getSystemApps(context))
        updateDeviceAppsCount()
    }

    fun addAllApps(context: Context) {
        apps4Cache.addAll(repository.getAllApps(context))
        updateDeviceAppsCount()
    }

    fun get(packageName: String): ApiViewingApp?{
        return apps4Cache.find { it.packageName == packageName }
    }

    fun screen4Display(loadItem: Int): List<ApiViewingApp> {
        synchronized(apps4Cache){
            return screen2DisplayPrivate(loadItem, apps4Cache)
        }
    }

    private fun screen2DisplayPrivate(loadItem: Int, info: List<ApiViewingApp> ): List<ApiViewingApp> {
        if (ApiUnit.ineffective(loadItem)) return info
        val unit = loadItem
        if (X.aboveOn(X.N)) {
            return if (loadItem == ApiUnit.ALL_APPS){
                info.parallelStream().collect(Collectors.toList())
            }else {
                info.parallelStream().filter{ item -> item.apiUnit == unit }.collect(Collectors.toList())
            }
        }else {
            val result: MutableList<ApiViewingApp> = mutableListOf()
            val iterator: Iterator<ApiViewingApp> = info.iterator()
            if (loadItem == ApiUnit.ALL_APPS){
                while (iterator.hasNext()){
                    val item = iterator.next()
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
            emptyList()
        }else {
            apps4Cache.filter { it.apiUnit != unit }
        })
    }

    fun sortApps(sortItem: Int) {
        updateApps4Cache(sortApps(apps4Cache, sortItem), false)
    }

    private fun sortApps(list: List<ApiViewingApp>, sortItem: Int): List<ApiViewingApp> {
        if (list.isEmpty()) return list
        var result: MutableList<ApiViewingApp> = list.toMutableList()
        when (sortItem) {
            ApiFragment.SORT_POSITION_API_LOW -> {
                if (X.aboveOn(Build.VERSION_CODES.N)) {
                    val comparator: java.util.Comparator<ApiViewingApp> = java.util.Comparator.comparingInt(ApiViewingApp::targetAPI)
                            .thenComparingDouble(ApiViewingApp::targetSDKDouble)
                            .thenComparing{o1, o2 ->
                                Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
                                        .compare(o1.name, o2.name)
                            }
                    result = result.parallelStream().sorted(comparator).collect(Collectors.toList())
                }else {
                    result.sortWith(Comparator { o1, o2 ->
                        val apiCompare = o1.targetAPI.compareTo(o2.targetAPI)
                        if (apiCompare != 0) return@Comparator apiCompare
                        val apiLevelCompare = o1.targetSDKDouble.compareTo(o2.targetSDKDouble)
                        if (apiLevelCompare != 0) return@Comparator apiLevelCompare
                        return@Comparator Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
                                .compare(o1.name, o2.name)
                    })
                }
            }
            ApiFragment.SORT_POSITION_API_HIGH -> {
                if (X.aboveOn(Build.VERSION_CODES.N)) {
                    val comparator: java.util.Comparator<ApiViewingApp> = java.util.Comparator.comparingInt(ApiViewingApp::targetAPI)
                            .thenComparingDouble(ApiViewingApp::targetSDKDouble)
                            .reversed()
                            .thenComparing { o1, o2 ->
                                Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
                                        .compare(o1.name, o2.name)
                            }
                    result = result.parallelStream().sorted(comparator).collect(Collectors.toList())
                }else {
                    result.sortWith(Comparator { o1, o2 ->
                        val apiCompare = o2.targetAPI.compareTo(o1.targetAPI)
                        if (apiCompare != 0) return@Comparator apiCompare
                        val apiLevelCompare = o2.targetSDKDouble.compareTo(o1.targetSDKDouble)
                        if (apiLevelCompare != 0) return@Comparator apiLevelCompare
                        return@Comparator Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
                                .compare(o1.name, o2.name)
                    })
                }
            }
            ApiFragment.SORT_POSITION_API_NAME ->
                result.sortWith(Comparator { o1, o2 -> Collator.getInstance(Locale.SIMPLIFIED_CHINESE).compare(o1.name, o2.name) })
            ApiFragment.SORT_POSITION_API_TIME ->
                if (X.aboveOn(Build.VERSION_CODES.N)) {
                    val comparator: java.util.Comparator<ApiViewingApp> = java.util.Comparator.comparingLong(ApiViewingApp::updateTime)/*
                            .thenComparingInt(APIApp::targetAPI)
                            .thenComparingDouble(APIApp::targetSDK)*/
                            .reversed()
                            .thenComparing { o1, o2 ->
                                Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
                                        .compare(o1.name, o2.name)
                            }
                    result = result.parallelStream().sorted(comparator).collect(Collectors.toList())
                }else {
                    result.sortWith(Comparator { o1, o2 ->
                        val compareTime = o2.updateTime.compareTo(o1.updateTime)
                        if (compareTime != 0) return@Comparator compareTime
                        return@Comparator Collator.getInstance(Locale.SIMPLIFIED_CHINESE)
                                .compare(o1.name, o2.name)
                    })
                }
        }
        return result
    }
}
