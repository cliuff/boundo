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

package com.madness.collision.unit.api_viewing

import android.util.SparseIntArray
import com.madness.collision.unit.api_viewing.data.ApiUnit
import com.madness.collision.unit.api_viewing.data.ApiViewingApp

class AppListStats {
    var appsCountUser: Int = 0
    var appsCountSystem: Int = 0
    val appsCountAll: Int
        get() = appsCountUser + appsCountSystem
    var apiCountUser: SparseIntArray = SparseIntArray(0)
    var apiCountSystem: SparseIntArray = SparseIntArray(0)
    var apiCountAll: SparseIntArray = SparseIntArray(0)
    var minApiCountUser: SparseIntArray = SparseIntArray(0)
    var minApiCountSystem: SparseIntArray = SparseIntArray(0)
    var minApiCountAll: SparseIntArray = SparseIntArray(0)
}

class AppStatsTracker {
    fun updateDeviceAppsCount(appList: List<ApiViewingApp>): AppListStats {
        var countAppsSystem = 0
        var countAppsUser = 0
        val apiCountMapUser: MutableMap<Int, Int> = mutableMapOf()
        val apiCountMapSystem: MutableMap<Int, Int> = mutableMapOf()
        val minApiCountMapUser: MutableMap<Int, Int> = mutableMapOf()
        val minApiCountMapSystem: MutableMap<Int, Int> = mutableMapOf()
        appList.forEach {
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
        return AppListStats().apply {
            appsCountUser = countAppsUser
            appsCountSystem = countAppsSystem
            getSections(apiCountMapUser, apiCountMapSystem).let { (usr, sys, all) ->
                apiCountUser = usr
                apiCountSystem = sys
                apiCountAll = all
            }
            getSections(minApiCountMapUser, minApiCountMapSystem).let { (usr, sys, all) ->
                minApiCountUser = usr
                minApiCountSystem = sys
                minApiCountAll = all
            }
        }
    }

    private fun getSections(userApiCount: Map<Int, Int>, sysApiCount: Map<Int, Int>) = kotlin.run {
        val appApiAll = SparseIntArray(userApiCount.size + sysApiCount.size)
        val appApiUser = SparseIntArray(userApiCount.size)
        userApiCount.forEach {
            appApiUser.put(it.key, it.value)
            val c = if (appApiAll.indexOfKey(it.key) != -1) appApiAll.get(it.key) else 0
            appApiAll.put(it.key, c + it.value)
        }

        val appApi = SparseIntArray(sysApiCount.size)
        sysApiCount.forEach {
            appApi.put(it.key, it.value)
            val c = if (appApiAll.indexOfKey(it.key) != -1) appApiAll.get(it.key) else 0
            appApiAll.put(it.key, c + it.value)
        }
        Triple(appApiUser, appApi, appApiAll)
    }
}
