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

package com.madness.collision.unit.api_viewing.origin

import android.content.Context
import android.content.pm.PackageInfo
import androidx.lifecycle.LifecycleOwner
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import com.madness.collision.unit.api_viewing.database.AppMaintainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking

/**
 * Retrieve apps from user's device
 */
internal class AppRetriever(private val context: Context, private val lifecycleOwner: LifecycleOwner)
    : OriginRetriever<ApiViewingApp> {

    companion object {
        suspend fun mapToApp(list: List<PackageInfo>, context: Context, anApp: ApiViewingApp)
        : List<ApiViewingApp> = coroutineScope {
            val lastIndex = list.lastIndex
            list.mapIndexed { index, packageInfo ->
                async(Dispatchers.Default) {
                    val app = if (index == lastIndex) anApp else anApp.clone() as ApiViewingApp
                    app.init(context, packageInfo, preloadProcess = true, archive = false)
                    app
                }
            }.map { it.await() }
        }
    }

    override fun get(predicate: ((PackageInfo) -> Boolean)?): List<ApiViewingApp> {
        return runBlocking { getAsync(predicate) }
    }

    /**
     * Get app in parallel.
     */
    private suspend fun getAsync(predicate: ((PackageInfo) -> Boolean)?)
    : List<ApiViewingApp> = coroutineScope {
        val packages = PackageRetriever(context).get(predicate)
        val anApp = AppMaintainer.get(context, lifecycleOwner)
        mapToApp(packages, context, anApp)
    }
}
