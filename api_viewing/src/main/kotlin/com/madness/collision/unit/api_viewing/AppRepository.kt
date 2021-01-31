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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import androidx.annotation.WorkerThread
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.data.EasyAccess
import com.madness.collision.unit.api_viewing.database.AppDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

internal class AppRepository(private val dao: AppDao){
    @WorkerThread
    suspend fun insert(app: ApiViewingApp) {
        dao.insert(app)
    }

    fun getUserApps(context: Context): List<ApiViewingApp>{
        return getAppsUser(context)
//        return dao.getUserApps().value ?: emptyList()
    }

    fun getSystemApps(context: Context): List<ApiViewingApp>{
        return getAppsSys(context)
//        return dao.getSystemApps().value ?: emptyList()
    }

    fun getAllApps(context: Context): List<ApiViewingApp>{
        return getAppsAll(context)
//        return dao.getAllApps().value ?: emptyList()
    }

    /**
     * Get app in parallel.
     */
    private suspend fun getAppsAsync(context: Context, predicate: ((PackageInfo) -> Boolean)? = null)
    : List<ApiViewingApp> = coroutineScope {
        val anApp = ApiViewingApp("")
        val packages = context.packageManager.getInstalledPackages(0).let {
            if (predicate == null) it else it.filter(predicate)
        }
        packages.mapIndexed { index, pack ->
            async(Dispatchers.Default) {
                val app = if (index == packages.lastIndex) anApp else anApp.clone() as ApiViewingApp
                app.packageName = pack.packageName
                app.init(context, pack, preloadProcess = true, archive = false)
                app
            }
        }.map { it.await() }
    }

    private fun getApps(context: Context, predicate: ((PackageInfo) -> Boolean)? = null): List<ApiViewingApp>{
        return context.packageManager.getInstalledPackages(0).let {
            if (predicate == null) it else it.filter(predicate)
        }.map { ApiViewingApp(context, it, preloadProcess = true, archive = false) }
    }

    private fun getAppsUser(context: Context): List<ApiViewingApp> {
        val predicate: (PackageInfo) -> Boolean = if (EasyAccess.shouldIncludeDisabled) { packageInfo ->
            (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        } else { packageInfo ->
            val app = packageInfo.applicationInfo
            // users without root privilege can only disable system apps
            (app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 && app.enabled
        }
        return runBlocking { getAppsAsync(context, predicate) }
    }

    private fun getAppsSys(context: Context): List<ApiViewingApp> {
        val predicate: (PackageInfo) -> Boolean = if (EasyAccess.shouldIncludeDisabled) { packageInfo ->
            (packageInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } else { packageInfo ->
            val app = packageInfo.applicationInfo
            (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && app.enabled
        }
        return runBlocking { getAppsAsync(context, predicate) }
    }

    private fun getAppsAll(context: Context): List<ApiViewingApp> {
        val predicate: ((PackageInfo) -> Boolean)? = if (EasyAccess.shouldIncludeDisabled) null
        else { packageInfo ->
            packageInfo.applicationInfo.enabled
        }
        return runBlocking { getAppsAsync(context, predicate) }
    }
}
