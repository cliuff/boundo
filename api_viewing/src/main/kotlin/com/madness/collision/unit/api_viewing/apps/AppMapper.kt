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

package com.madness.collision.unit.api_viewing.apps

import android.content.Context
import android.content.pm.PackageInfo
import com.madness.collision.unit.api_viewing.data.ApiViewingApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

suspend fun List<PackageInfo>.toPkgApps(context: Context, anApp: ApiViewingApp) = coroutineScope {
    val lastIndex = lastIndex
    mapIndexed { index, packageInfo ->
        async(Dispatchers.Default) {
            val app = if (index == lastIndex) anApp else anApp.clone() as ApiViewingApp
            app.init(context, packageInfo, preloadProcess = true, archive = false)
            app
        }
    }.map { it.await() }
}

suspend fun List<*>.toRecApps(context: Context, anApp: ApiViewingApp) = coroutineScope {
    val lastIndex = lastIndex
    mapIndexedNotNull { index, item ->
        if (item !is ApiViewingApp) return@mapIndexedNotNull null
        async(Dispatchers.Default) {
            val app = if (index == lastIndex) anApp else anApp.clone() as ApiViewingApp
            app.assignPersistentFieldsFrom(item, deepCopy = false)
            // get application info one at a time, may be optimized
            app.initIgnored(context)
            app
        }
    }.map { it.await() }
}
